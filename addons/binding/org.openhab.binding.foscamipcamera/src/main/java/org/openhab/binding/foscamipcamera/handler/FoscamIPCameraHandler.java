/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.foscamipcamera.handler;

import static org.openhab.binding.foscamipcamera.FoscamIPCameraBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.foscamipcamera.internal.FoscamIPCameraConfiguration;
import org.openhab.binding.foscamipcamera.tls.LenientSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The {@link FoscamIPCameraHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author ricardo.lorenzo - Initial contribution
 */
 @NonNullByDefault
public class FoscamIPCameraHandler extends BaseThingHandler {
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService serviceCached = Executors.newCachedThreadPool();
    private long polltime_ms = 5000;
    private final Logger logger = LoggerFactory.getLogger(FoscamIPCameraHandler.class);

    @Nullable
    private FoscamIPCameraConfiguration config;

    public FoscamIPCameraHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if(CHANNEL_PTZ_CONTROL.equals(channelUID.getId())) {
            try {
                if(PTZ_CONTROL_VALID_COMMANDS.contains(command.toString())) {
                    Integer moveResult = Integer.valueOf(getXMLTag(readContent(
                        new URL(getCameraCommandUri(command.toString()))),"result"));
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    logger.warn("could not handle command: {}", getThing(), command.toString());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "it is not a valid command: " + command.toString());
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(FoscamIPCameraConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        logger.debug("Initialize thing: {}::{}", getThing().getLabel(), getThing().getUID());
        try {
            if(config.pollingSeconds != null && config.pollingSeconds > 0) {
                polltime_ms = Double.valueOf(config.pollingSeconds * 1000).longValue();
            }
        } catch (Exception e1) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            logger.warn("could not read polling time (pollingSeconds) from configuration", e1);
        }
        logger.debug("Schedule update at fixed rate {} ms.", polltime_ms);
        if (initialized.compareAndSet(false, true)) {
            WeakReference<FoscamIPCameraHandler> weakReference = new WeakReference<>(this);
            serviceCached.submit(new Callable<Object>() {
                @Override
                public Void call() throws Exception {
                    while (weakReference.get() != null) {
                        refreshData();
                        Thread.sleep(Math.max(1000, polltime_ms));
                    }
                    return null;
                }
            });
        }
        updateStatus(ThingStatus.OFFLINE);
    }

    private String getCameraCommandUri(String cmd) {
        StringBuilder uri = new StringBuilder();
        if(config.cameraNetworkTLS) {
            uri.append("https://");
        } else {
            uri.append("http://");
        }
        uri.append(config.cameraNetworkAddress);
        if(config.cameraNetworkPort != null) {
            uri.append(":");
            uri.append(config.cameraNetworkPort);
        }
        uri.append("/cgi-bin/CGIProxy.fcgi?");
        uri.append("usr=");
        uri.append(config.cameraUsername);
        uri.append("&pwd=");
        uri.append(config.cameraPassword);
        uri.append("&cmd=");
        uri.append(cmd);
        return uri.toString();
    }

    private void refreshData() {
        if (refreshInProgress.compareAndSet(false, true)) {
            try {
                byte[] deviceInfoData = null;
                for (Channel cx : getThing().getChannels()) {
                    switch(cx.getUID().getId()) {
                        case CHANNEL_PRODUCT_NAME:
                        case CHANNEL_PRODUCT_FIRMWARE:
                        case CHANNEL_DEVICE_NAME:
                            if(deviceInfoData == null) {
                                try {
                                    deviceInfoData = readContent(new URL(getCameraCommandUri("getDevInfo")));
                                    updateStatus(ThingStatus.ONLINE);
                                } catch (Exception e) {
                                    handleException(e);
                                }
                            }
                            try {
                                if(CHANNEL_PRODUCT_NAME.equals(cx.getUID().getId())) {
                                    updateState(cx.getUID(), new StringType(getXMLTag(deviceInfoData, "productName")));
                                } else if(CHANNEL_PRODUCT_FIRMWARE.equals(cx.getUID().getId())) {
                                    updateState(cx.getUID(), new StringType(getXMLTag(deviceInfoData, "firmwareVer")));
                                } else if(CHANNEL_DEVICE_NAME.equals(cx.getUID().getId())) {
                                    updateState(cx.getUID(), new StringType(getXMLTag(deviceInfoData, "devName")));
                                }
                            } catch (Exception e) {
                                handleException(e);
                            }
                            break;
                        case CHANNEL_MOTION:
                            logger.trace("Will update: {}::{}::{}", getThing().getUID().getId(),
                                cx.getChannelTypeUID().getId(), getThing().getLabel());
                            try {
                                Integer motionDetectedAlarm = Integer.valueOf(getXMLTag(readContent(
                                    new URL(getCameraCommandUri("getDevState"))),"motionDetectAlarm"));
                                /**
                                 * MotionDetectAlarm
                                 *
                                 * 0 - Disabled
                                 * 1 - No alarm
                                 * 2 - Alarm detected
                                 */
                                if(motionDetectedAlarm == 2) {
                                    updateState(cx.getUID(), OnOffType.ON);
                                } else {
                                    updateState(cx.getUID(), OnOffType.OFF);
                                }
                                updateStatus(ThingStatus.ONLINE);
                            } catch (Exception e) {
                                handleException(e);
                            }
                            break;
                        case CHANNEL_SNAPSHOT:
                            logger.trace("Will update: {}::{}::{}", getThing().getUID().getId(),
                                cx.getChannelTypeUID().getId(), getThing().getLabel());
                            try {
                                updateState(cx.getUID(), new RawType(
                                    readContent(new URL(getCameraCommandUri("snapPicture2"))), "image/jpeg"));
                                updateStatus(ThingStatus.ONLINE);
                            } catch (Exception e) {
                                handleException(e);
                            }
                            break;
                    }
                }
            } finally {
                refreshInProgress.set(false);
            }
        }
    }

    private void handleException(Exception exception) {
        try {
            throw exception;
        } catch (NumberFormatException e) {
            logger.warn("could not update value: {}", getThing(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "invalid format returned: " + e.toString());
        } catch (MalformedURLException e) {
            logger.warn("could not update value: {}", getThing(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "url not valid: " + e.toString());
        } catch (IOException e) {
            logger.warn("could not update value: {}", getThing(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "camera not reachable: " + e.toString());
        } catch (Exception e) {
            logger.warn("could not update value: {}", getThing(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE,
                "unknown error: " + e.toString());
        }

    }

    private byte[] readContent(URL uri) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream response;
        if(config.cameraNetworkTLS) {
            HttpsURLConnection connection = HttpsURLConnection.class.cast(uri.openConnection());
            if(config.disableHostnameValidation) {
                connection.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }
            if(config.disableCertificateValidation) {
                connection.setSSLSocketFactory(new LenientSSLSocketFactory());
            }
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Connection", "close");
            connection.setRequestProperty("User-Agent", BINDING_DESCRIPTION);
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + connection.getResponseCode());
            }
            response = connection.getInputStream();
        } else {
            HttpURLConnection connection = HttpURLConnection.class.cast(uri.openConnection());
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Connection", "close");
            connection.setRequestProperty("User-Agent", BINDING_DESCRIPTION);
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + connection.getResponseCode());
            }
            response = connection.getInputStream();
        }

        try {
            byte[] buffer = new byte[4096];
            for (int n; (n = response.read(buffer)) > 0;) {
                baos.write(buffer, 0, n);
            }
        } finally {
            response.close();
        }
        return baos.toByteArray();
    }

    private String getXMLTag(byte[] data, String tag) throws Exception {
        if(data == null) {
            return "";
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(data));
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName(tag);
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) nNode;
                return element.getTextContent();
            }
        }
        return "";
    }
}
