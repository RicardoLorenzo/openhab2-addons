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
package org.openhab.binding.foscamipcamera;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import java.util.HashSet;
import java.util.Set;


/**
 * The {@link FoscamIPCameraBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author ricardo.lorenzo - Initial contribution
 */
 @NonNullByDefault
public class FoscamIPCameraBindingConstants {

    private static final String BINDING_ID = "foscamipcamera";
    public static final String BINDING_DESCRIPTION = "OpenHab2 Foscam Camera Binding";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_CAMERA = new ThingTypeUID(BINDING_ID, "camera");

    // List of all Channel ids
    public static final String CHANNEL_PRODUCT_NAME = "product_name";
    public static final String CHANNEL_PRODUCT_FIRMWARE = "product_firmware";
    public static final String CHANNEL_DEVICE_NAME = "device_name";
    public static final String CHANNEL_MOTION = "motion_detection";
    public static final String CHANNEL_SNAPSHOT = "image_snapshot";
    public static final String CHANNEL_PTZ_CONTROL = "ptz_control";

    public static Set<String> PTZ_CONTROL_VALID_COMMANDS = new HashSet<>();
    static {
        PTZ_CONTROL_VALID_COMMANDS.add("ptzReset");
        PTZ_CONTROL_VALID_COMMANDS.add("ptzMoveUp");
        PTZ_CONTROL_VALID_COMMANDS.add("ptzMoveDown");
        PTZ_CONTROL_VALID_COMMANDS.add("ptzMoveRight");
        PTZ_CONTROL_VALID_COMMANDS.add("ptzMoveLeft");
    }

}
