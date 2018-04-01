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
package org.openhab.binding.foscamipcamera.internal;

/**
 * The {@link FoscamIPCameraConfiguration} class contains fields mapping thing configuration paramters.
 *
 * @author ricardo.lorenzo - Initial contribution
 */
public class FoscamIPCameraConfiguration {

    /**
     * Camera network ip address or hostname
     */
    public String cameraNetworkAddress;

    /**
     * Camera network
     */
    public String cameraNetworkPort;

    /**
     * Camera TLS option
     */
    public Boolean cameraNetworkTLS;

    /**
     * Camera user
     */
    public String cameraUsername;

    /**
     * Camera password
     */
    public String cameraPassword;
}
