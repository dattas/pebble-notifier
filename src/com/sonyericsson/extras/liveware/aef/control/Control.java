/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (C) 2012-2013 Sony Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

* Neither the name of the Sony Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.sonyericsson.extras.liveware.aef.control;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;


/**
 * <h1>Control API is a part of the Smart Extension APIs</h1>
 * <p>
 * Some of our Smart accessories will support the Control API.
 * The Control API enables the Extension to take total control of the accessory.
 * It takes control over the display, LEDs, vibrator, input events.
 * Because of this, only one Extension can run in this mode at a time.
 * </p>
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Registration">Registration</a>
 * <li><a href="#Lifecycle">Extension lifecycle</a>
 * <li><a href="#DisplayControl">Controlling the display</a>
 * <li><a href="#LEDControl">Controlling the LEDs</a>
 * <li><a href="#VibratorControl">Controlling the vibrator</a>
 * <li><a href="#KeyEvents">Key events</a>
 * <li><a href="#TouchEvents">Touch events</a>
 * <li><a href="#DataSending">Displaying content on the accessory display</a>
 * </ol>
 * </p>
 * <a name="Registration"></a>
 * <h3>Registration</h3>
 * <p>
 * Before a Control Extension can use an accessory, it must use the registration API
 * content provider to insert a record in the extension table. It must also register
 * information in the registration table. This must be done for each Host Application
 * that the Extension wants to interact with.
 * </p>
 * <p>
 * In order to find out what Host Applications are available and what capabilities they
 * support, the Extension should use the Capability API.
 * </p>
 * <a name="Lifecycle"></a>
 * <h3>Extension lifecycle</h3>
 * <p>
 * After a successful registration the Extension can start communicating with the Host
 * Application. Since an Extension implementing this API takes complete control over the
 * accessory only one Extension can run at a time.
 * </p>
 * <p>
 * An Extension cannot just start executing whenever it wants, it needs to make sure that
 * no other Extension is running, therefore the Extension can only request to be started,
 * {@link Intents#CONTROL_START_REQUEST_INTENT}. When the Host Application is ready to give
 * control to the Extension it will send a {@link Intents#CONTROL_START_INTENT}, see figure
 * below.
 * </p>
 * <p>
 * <img src="../../../../../../../images/control_api_extension_lifecycle.png"
 * alt="Extension lifecycle" border="1" />
 * </p>
 * <p>
 * When the Extension requests to start controlling the accessory the Host Application can
 * either accept the request and give control to the Extension, or if something is not right
 * the Host Application can send a {@link Intents#CONTROL_ERROR_INTENT}. See
 * {@link Intents#EXTRA_ERROR_CODE} for different error codes that the Host Application can send.
 * </p>
 * <p>
 * The {@link Intents#CONTROL_RESUME_INTENT} is sent when the Extension is visible on the accessory.
 * From this point on the Extension controls everything, the Host Application just forwards the
 * information between the accessory and the Extension.
 * </p>
 * <p>
 * An Extension can also be paused, either if a high priority Extension needs to run for a
 * while or if the Host Application is in charge of the display state and the display is
 * turned off. In this case the Host Application sends a {@link Intents#CONTROL_PAUSE_INTENT}
 * to the Extension. This means that there is no point for the Extension to update the display
 * since it is either turned off or someone else has control over it. If the Extension would
 * break this rule and try to update the display anyway, the Host Application will ignore these
 * calls.
 * </p>
 * <p>
 * When the Extension is in a paused state it no longer has control over the display/LEDs/
 * vibrator/key events. As an example one could say that a telephony Extension has high priority.
 * E.g. when a random Extension is running and the user receives a phone call. We want to pause
 * the running Extension and let the telephony Extension display the caller id on the accessory
 * display. When the phone call ends the telephony Extension is done and the other Extension can
 * resume its running, it will then receive a {@link Intents#CONTROL_RESUME_INTENT}.
 * </p>
 * <p>
 * When the {@link Intents#CONTROL_RESUME_INTENT} is sent from a Host Application the Extension is
 * once again in charge of everything.
 * </p>
 * <p>
 * When the user chooses to exit the Extension the Host Application will send a
 * {@link Intents#CONTROL_PAUSE_INTENT} followed by a {@link Intents#CONTROL_STOP_INTENT}.
 * From this point on the Host Application regains control.
 * </p>
 * <p>
 * If the Extension would like to stop itself when running, like the telephony Extension, it can
 * send a {@link Intents#CONTROL_STOP_REQUEST_INTENT} to the Host Application. The Host Application
 * will then make sure to stop it and send a {@link Intents#CONTROL_STOP_INTENT}.
 * If the extension was not already paused the it will be paused before it is stopped and a
 * {@link Intents#CONTROL_PAUSE_INTENT} is sent before the {@link Intents#CONTROL_STOP_INTENT}.
 * In case another Extension has been paused it will be resumed.
 * </p>
 *
 * <a name="DisplayControl"></a>
 * <h3>Controlling the display</h3>
 * <p>
 * Extensions implementing this API have the possibility to control the state of the accessory
 * display.The display can be controlled via {@link Intents#CONTROL_SET_SCREEN_STATE_INTENT}.
 * </p>
 * <p>
 * It is important that you program your Extension so that it consumes as little power as possible,
 * both on the phone side and on the accessory. The accessory has a much smaller battery then the
 * phone so use this functionality with caution. When possible, let the Host Application take control
 * of the display state. That way you don't have to bother about the power consumption on the accessory.
 * You can do this by setting the display state to "Auto".
 * </p>
 * <p>
 * By default when your Extension starts the display state will be set to "Auto", which means that the
 * Host Application controls the on/off/dim behavior. If the Extension wants to control the display state
 * it must explicitly change the state.
 * </p>
 * <p>
 * If the Extension controls the display state and you get a {@link Intents#CONTROL_STOP_INTENT}, meaning
 * your Extension is no longer running, the Host Application will automatically take over the display
 * control.
 * </p>
 * <p>
 * Note that when in "Auto" mode, the Extension will receive a {@link Intents#CONTROL_PAUSE_INTENT} when
 * display is off and a {@link Intents#CONTROL_RESUME_INTENT} when the display goes back on.
 * </p>
 * <h4>Active power save mode</h4>
 * <p>
 * Some accessories may support an additional display mode where information can be shown to the
 * user while keeping the battery consumption to a minimum. In this active power save mode the
 * display only supports monochrome (black and white) display content.
 * Accessories with support for active power save mode indicates this in
 * {@link Registration.DisplayColumns#SUPPORTS_LOW_POWER_MODE}.
 * If the control extension wants use the active power save mode it must set the
 * {@link Registration.ApiRegistration#LOW_POWER_SUPPORT} to TRUE when registering with a Host
 * Application.
 * </p>
 * <p>
 * If both the extension and the accessory support active power save mode, this mode will be
 * activated when the screen would otherwise go off.
 * This means that if screen state is {@link Intents#SCREEN_STATE_AUTO} the accessory will decide when to
 * enter active power save mode.
 * If screen state is {@link Intents#SCREEN_STATE_ON} or {@link Intents#SCREEN_STATE_DIM} the
 * extension can put the display in active power save mode by setting the screen state to
 * {@link Intents#SCREEN_STATE_OFF}.
 * The {@link Intents#CONTROL_ACTIVE_POWER_SAVE_MODE_STATUS_CHANGED_INTENT} intent is sent to the
 * extension when the display enters active power save mode both when the active power save
 * mode is initiated by the accessory and extension.
 * When in active power save mode the extension is expected to provide monochrome display
 * content through the same intents as in normal display mode.
 * The extension can update the display in the same ways as in normal display mode.
 * </p>
 * <p>
 * If the screen state is {@link Intents#SCREEN_STATE_AUTO} and the display was put in active
 * power save mode by the accessory, the accessory also decides when to leave the active
 * power save mode.
 * In this mode the extension will not receive any input events from the accessory as these
 * will cause the display to leave the active power save mode.
 * If the display was put in active power save mode by the control extension it is the
 * responsibility of the control extension to decide when to leave the active power save mode
 * by setting the screen state to {@link Intents#SCREEN_STATE_ON}.
 * If the extension wants to get input events when the screen is in active power save mode, it must
 * manually put the display in active power save mode.
 * When the display leaves the active power save mode a
 * {@link Intents#CONTROL_ACTIVE_POWER_SAVE_MODE_STATUS_CHANGED_INTENT} will always be sent to
 * the extension and the extension is expected by update the display with new content.
 * </p>
 * <a name="LEDControl"></a>
 * <h3>Controlling the LEDs</h3>
 * <p>
 * The accessory might have one or more LEDs that are used to notify the user about events. The
 * Extension can find information about the LEDs for a certain accessory via the Registration &amp;
 * Capability API.
 * </p>
 * <p>
 * If the accessory has LEDs, the Extension can control them via the Control API. The LEDs can be
 * controlled via the {@link Intents#CONTROL_LED_INTENT}.
 * Note that the Host Application might overtake the control of the LED at any time if it wants to
 * show some important notifications to the user, e.g. when the accessory battery level is low.
 * The Extension is unaware of this so it might still try to control the LEDs but the Host
 * Application will ignore the calls.
 * </p>
 * <a name="VibratorControl"></a>
 * <h3>Controlling the vibrator</h3>
 * <p>
 * Our accessories might or might not have a vibrator. The Extension can find this out by checking
 * the capabilities of the Host Application via the Registration &amp; Capability API. If the accessory
 * has a vibrator it is controllable via the Control API, {@link Intents#CONTROL_VIBRATE_INTENT}.
 * </p>
 * <a name="KeyEvents"></a>
 * <h3>Key events</h3>
 * <p>
 * The accessory might have several hardware keys. Your extension will receive the key events when
 * one of the keys is pressed. The {@link Intents#CONTROL_KEY_EVENT_INTENT} is sent to the Extension when
 * a user presses a key on the accessory.
 * </p>
 * <p>
 * The Intent carries a few parameters, such as the time stamp of the event, the type of event
 * (press, release and repeat) and also the key code. The accessory might have one or more keypads
 * defined. Extensions can look this up in the Registration &amp; Capabilities API. Each key will have a
 * unique key code for identification. Key codes can be found in the product SDK.
 * </p>
 * <a name="TouchEvents"></a>
 * <h3>Touch events</h3>
 * <p>
 * Certain accessories might have a touch display. Extensions can find this information using the
 * Registration &amp; Capabilities API. The {@link Intents#CONTROL_TOUCH_EVENT_INTENT} is sent to the
 * Extension when a user taps the accessory display.
 * </p>
 * <p>
 * If the {@link Intents#CONTROL_DISPLAY_DATA_INTENT} is used to send images, then touch events with
 * display coordinates are delivered in the {@link Intents#CONTROL_TOUCH_EVENT_INTENT} intents.
 * If a swipe gesture is detected then a {@link Intents#CONTROL_SWIPE_EVENT_INTENT} is sent to the
 * Extension instead.
 * </p>
 * <p>
 * If the {@link Intents#CONTROL_PROCESS_LAYOUT_INTENT} is used to send layouts then some Views
 * in the layout may handle the touch events themselves.
 * Touch events are for example handled by views that have android:clickable set to to true.
 * For these views the extension is informed about clicks through the
 * {@link Intents#CONTROL_OBJECT_CLICK_EVENT_INTENT} intent.
 * ListViews also handle touch event and report clicks in {@link Intents#CONTROL_LIST_ITEM_CLICK_INTENT}
 * intents.
 * Touch events and swipe gestures that are not handled by Views in the layout are sent to the
 * Extension through {@link Intents#CONTROL_TOUCH_EVENT_INTENT} and
 * {@link Intents#CONTROL_SWIPE_EVENT_INTENT} intents.
 * </p>
 * <a name="DataSending"></a>
 * <h3>Displaying content on the accessory display</h3>
 * <p>
 * Since the Extension is controlling the accessory it also controls what is visible on the display.
 * The content visible to the user comes from the Extension. Basically the Extension sends images to
 * be displayed on the accessory display. To find out the dimensions of the display and the color depth
 * it supports the Extension can use the Registration &amp; Capabilities API. The
 * {@link Intents#CONTROL_DISPLAY_DATA_INTENT} is sent from the Extension when it wants to update the accessory
 * display. Extensions can also clear the accessory display at any point if they want to by sending
 * the {@link Intents#CONTROL_CLEAR_DISPLAY_INTENT}.
 * </p>
 * <p>
 * The Extension can send images as raw data (byte array) or it can just send the URI of the image to
 * be displayed. Note that we are using Bluetooth as bearer which means that we can't send that many
 * frames per second (FPS). Refresh rate of the display can be found in the Registration &amp; Capabilities API.
 * </p>
 * <a name="Layouts"/>
 * <h3>Layouts</h3>
 * <p>
 * Starting with version 2 of the Control API it is possible to send layouts to the accessory as an
 * alternative to sending images.
 * The subset of Android layouts that are supported is specified in {@link Intents#EXTRA_DATA_XML_LAYOUT}.
 * Layouts are sent using {@link Intents#CONTROL_PROCESS_LAYOUT_INTENT}.
 * The contents of the views in the layouts can be updated using {@link Intents#CONTROL_SEND_IMAGE_INTENT}
 * and {@link Intents#CONTROL_SEND_TEXT_INTENT}.
 * When using layouts, click events are delivered as {@link Intents#CONTROL_OBJECT_CLICK_EVENT_INTENT} intents.
 * </p>
 * <a name="Lists"/>
 * <h4>Lists</h4>
 * <p>
 * A layout may include a ListView.
 * The ListView is initiated by sending a {@link Intents#CONTROL_LIST_COUNT_INTENT}.
 * This intent can include the list items in the {@link Intents#EXTRA_LIST_CONTENT}.
 * If no {@link Intents#EXTRA_LIST_CONTENT} is provided the Host Application will request
 * individual list items when needed through {@link Intents#CONTROL_LIST_REQUEST_ITEM_INTENT}.
 * The Control extension can refresh the list content at any time by sending a new
 * {@link Intents#CONTROL_LIST_COUNT_INTENT} intent.
 * This can be done both if additional items should be added or just if the existing items
 * should be refreshed.
 * The Control extension is notified about clicks on list items through the
 * {@link Intents#CONTROL_LIST_ITEM_CLICK_INTENT} intent.
 * </p>
 * <p>
 * A ListView and its items must always fill the entire display width.
 * The height of a list item must be less or equal to the height of the ListView.
 * </p>
 * <p>
 * Some lists may support a user initiated refresh.
 * The {@link Intents#CONTROL_LIST_REFRESH_REQUEST_INTENT} is sent to the extension if
 * the user performs a manual refresh action.
 * The extension is expected to check its data source (for example trigger a poll to a server)
 * and update the list content.
 * If the number of items is changed a new {@link Intents#CONTROL_LIST_COUNT_INTENT} should be sent.
 * If only the existing list items should be updated this is done through a number of
 * {@link Intents#CONTROL_LIST_ITEM_INTENT} intents.
 * </p>
 * <a name="Gallery"/>
 * <h4>Gallery</h4>
 * <p>
 * The Control extension interacts with the Gallery view in the same way as the ListView with the
 * addition that it is also notified about selected list items through the
 * {@link Intents#CONTROL_LIST_ITEM_SELECTED_INTENT} intent.
 * </p>
 * <p>
 * A Gallery and its items must always fill the entire display width.
 * The height of a list item must be less or equal to the height of the Gallery.
 * </p>
 */

public class Control {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     * that will be used by Control API Extension developers.
     */
    protected Control() {
    }

    /**
     * Intents sent between Control Extensions and Accessory Host Applications.
     */
    public interface Intents {

        /**
         * Intent sent by the Extension when it wants to take control of the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_START_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.START_REQUEST";

        /**
         * Intent sent by the Extension when it wants to stop controlling the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_STOP_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.STOP_REQUEST";

        /**
         * Intent sent by the Host Application when it grants control of the accessory display to the Extension.
         * This Intent might be sent when the Host Application wants to start the Extension or as a
         * result of the Extensions sending a {@link #CONTROL_START_REQUEST_INTENT} Intent.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_START_INTENT = "com.sonyericsson.extras.aef.control.START";

        /**
         * Intent sent by the Host Application when it takes back control of the accessory display from the Extension.
         * This Intent might be sent when the Host Application wants to stop the Extension or as a
         * result of the Extensions sending a {@link #CONTROL_STOP_REQUEST_INTENT} Intent.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_STOP_INTENT = "com.sonyericsson.extras.aef.control.STOP";

        /**
         * Intent sent by the Host Application when the Extension is no longer visible on the display.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_PAUSE_INTENT = "com.sonyericsson.extras.aef.control.PAUSE";

        /**
         * Intent sent by the Host Application when the Extension is visible on the display.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_RESUME_INTENT = "com.sonyericsson.extras.aef.control.RESUME";

        /**
         * Intent sent by the Host Application when a error occurs
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_ERROR_CODE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_ERROR_INTENT = "com.sonyericsson.extras.aef.control.ERROR";

        /**
         * Intent sent by the Extension when it wants to set the state of the accessory display.
         * If the Extension does not set the state explicitly it will by default be controlled by
         * the Host Application (Display Auto) for optimal power consumption.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SCREEN_STATE}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_SET_SCREEN_STATE_INTENT = "com.sonyericsson.extras.aef.control.SET_SCREEN_STATE";

        /**
         * Intent sent by the Extension when it wants to control one of the LEDs available on the accessory.
         * Every Host Application will expose information about its LEDs in the
         * Registration &amp; Capabilities API.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LED_ID}</li>
         * <li>{@link #EXTRA_LED_COLOR}</li>
         * <li>{@link #EXTRA_ON_DURATION}</li>
         * <li>{@link #EXTRA_OFF_DURATION}</li>
         * <li>{@link #EXTRA_REPEATS}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_LED_INTENT = "com.sonyericsson.extras.aef.control.LED";

        /**
         * Intent sent by the Extension when it wants to stop an ongoing LED sequence on the accessory.
         * If the LED specified in the Intent-extra if off, this Intent will be ignored by the Host Application.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LED_ID}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_STOP_LED_INTENT = "com.sonyericsson.extras.aef.control.STOP_LED";

        /**
         * Intent sent by the Extension when it wants to control the vibrator available on the accessory.
         * Every Host Application will expose information about the vibrator if it has one in the
         * Registration &amp; Capabilities API.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_ON_DURATION}</li>
         * <li>{@link #EXTRA_OFF_DURATION}</li>
         * <li>{@link #EXTRA_REPEATS}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_VIBRATE_INTENT = "com.sonyericsson.extras.aef.control.VIBRATE";

        /**
         * Intent sent by the Extension when it wants to stop an ongoing vibration on the accessory.
         * If no vibration is ongoing, this Intent will be ignored by the Host Application.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_STOP_VIBRATE_INTENT = "com.sonyericsson.extras.aef.control.STOP_VIBRATE";

        /**
         * Intent sent by the Extension whenever it wants to update the accessory display.
         * The display size is accessory dependent and can be found using the Registration &amp; Capabilities API.
         * <p>
         * The Accessory may support several displays.
         * The displays can be real displays or emulated displays to provide compatibility for
         * Extensions written for other Accessories.
         * If several displays are supported the Extension can use {@link #EXTRA_DISPLAY_ID}
         * to specify the target display.
         * If {@link #EXTRA_DISPLAY_ID} is absent the Host Application will make a selection
         * of target display and if necessary scale the image.
         * </p>
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_DATA_URI}</li>
         * <li>{@link #EXTRA_DATA}</li>
         * <li>{@link #EXTRA_X_OFFSET}</li>
         * <li>{@link #EXTRA_Y_OFFSET}</li>
         * <li>{@link #EXTRA_DISPLAY_ID} (optional)</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_DISPLAY_DATA_INTENT = "com.sonyericsson.extras.aef.control.DISPLAY_DATA";

        /**
         * Intent sent by the Extension whenever it wants to clear the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_CLEAR_DISPLAY_INTENT = "com.sonyericsson.extras.aef.control.CLEAR_DISPLAY";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an hardware
         * key is pressed/released.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_KEY_ACTION}</li>
         * <li>{@link #EXTRA_TIMESTAMP}</li>
         * <li>{@link #EXTRA_KEY_CODE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_KEY_EVENT_INTENT = "com.sonyericsson.extras.aef.control.KEY_EVENT";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an touch
         * event is detected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_TOUCH_ACTION}</li>
         * <li>{@link #EXTRA_TIMESTAMP}</li>
         * <li>{@link #EXTRA_X_POS}</li>
         * <li>{@link #EXTRA_Y_POS}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_TOUCH_EVENT_INTENT = "com.sonyericsson.extras.aef.control.TOUCH_EVENT";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an swipe
         * event is detected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SWIPE_DIRECTION}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        static final String CONTROL_SWIPE_EVENT_INTENT = "com.sonyericsson.extras.aef.control.SWIPE_EVENT";

        /**
         * Intent sent by the Extension whenever it wants to update the Accessory display by sending an XML layout.
         * Note that the extension must always send a layout covering the full screen according to the display screen size.
         * This means that even though the extension wants to update a portion of the screen a full screen layout must
         * be supplied anyway. Objects that should be displayed in the layout are sent separately.
         * <p>
         * {@link #EXTRA_LAYOUT_DATA} can be used to update view in the layout with new values.
         * The content of the view in the layout can also be updated using {@link #CONTROL_SEND_TEXT_INTENT} and {@link #CONTROL_SEND_IMAGE_INTENT}.
         * </p>
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_DATA_XML_LAYOUT}</li>
         * <li>{@link #EXTRA_LAYOUT_DATA} (optional)</li>
         * </ul>
         * </p>
         * @since 2.0
         */
        static final String CONTROL_PROCESS_LAYOUT_INTENT = "com.sonyericsson.extras.aef.control.PROCESS_LAYOUT";

        /**
         * Intent sent by the Control extension whenever it wants to update an image in an ImageView on the accessory.
         * The image can be a URI or an array of a raw image, like JPEG
         * The image will replace any previous sent image with the same reference.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_DATA_URI}</li>
         * <li>{@link #EXTRA_DATA}</li>
         * </ul>
         * </p>
         * @since 2.0
         */
        static final String CONTROL_SEND_IMAGE_INTENT = "com.sonyericsson.extras.aef.control.SEND_IMAGE";

        /**
         * Intent sent by the Control extension whenever it wants to update a text in a TextView on the accessory.
         * The text will replace any previous sent text with the same reference.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_TEXT}</li>
         * </ul>
         * </p>
         * @since 2.0
         */
        static final String CONTROL_SEND_TEXT_INTENT = "com.sonyericsson.extras.aef.control.SEND_TEXT";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever the active power save mode status changed.
         * This intent is only sent to control extensions that support active power save mode.
         * In active power save mode, the control should supply a black and white image.
         *
         * Depending on the mode, a suitable layout should be sent to the accessory.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_ACTIVE_POWER_MODE_STATUS}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 2.0
         */
        static final String CONTROL_ACTIVE_POWER_SAVE_MODE_STATUS_CHANGED_INTENT = "com.sonyericsson.extras.aef.control.ACTIVE_POWER_SAVE_MODE_STATUS_CHANGED";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever a click
         * event is detected on a graphical object referenced from a layout.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_CLICK_TYPE}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 2.0
         */
        static final String CONTROL_OBJECT_CLICK_EVENT_INTENT = "com.sonyericsson.extras.aef.control.OBJECT_CLICK_EVENT";

        /**
         * Intent sent by the Control extension when it wants to set the number
         * of items in a ListView. {@link #EXTRA_LAYOUT_REFERENCE} refers to the
         * ListView to be updated. This intent is used both to specify the
         * initial size of the list and to update the size of the list.
         * <p>
         * {@link #EXTRA_LIST_REFRESH_ALLOWED} specifies if the user is allowed
         * to manually initiate a refresh of the list content. The default
         * behavior is that the user is not allowed to initiate a refresh.
         * The extension is notified about a refresh request through the
         * {@link #CONTROL_LIST_REFRESH_REQUEST_INTENT} intent.
         * </p>
         * <p>
         * This intent should be sent with enforced security by supplying the
         * host application permission to sendBroadcast(Intent, String).
         * {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_LIST_REFRESH_ALLOWED} (optional)</li>
         * <li>{@link #EXTRA_LIST_COUNT}</li>
         * <li>{@link #EXTRA_LIST_CONTENT} (optional)</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_COUNT_INTENT = "com.sonyericsson.extras.aef.control.LIST_COUNT";


        /**
         * Intent sent by the Control extension when it wants to move to a
         * certain position in a list. The position to move to can either be
         * specified using {@link #EXTRA_LIST_ITEM_POSITION} or
         * {@link #EXTRA_LIST_ITEM_ID}.
         * <p>
         * This intent should be sent with enforced security by supplying the
         * host application permission to sendBroadcast(Intent, String).
         * {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_LIST_ITEM_ID} or {@link #EXTRA_LIST_ITEM_POSITION}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_MOVE_INTENT = "com.sonyericsson.extras.aef.control.LIST_MOVE";

        /**
         * Intent sent the by the Host Application to the Control extension as a
         * request for a list item.
         * The extension is expected to respond with a {@link #CONTROL_LIST_ITEM_INTENT}.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_LIST_ITEM_POSITION}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_REQUEST_ITEM_INTENT = "com.sonyericsson.extras.aef.control.LIST_REQUEST_ITEM";

        /**
         * Intent sent by the Control extension to update a list item in a
         * ListView. This can be a response to
         * {@link #CONTROL_LIST_REQUEST_ITEM_INTENT}, but it can also be sent
         * unsolicited to refresh an individual list item. To refresh an entire
         * list a new {@link #CONTROL_LIST_COUNT_INTENT} can be used.
         * <p>
         * {@link #EXTRA_LAYOUT_REFERENCE} specifies the ListView and
         * {@link #EXTRA_LIST_ITEM_POSITION} specifies the position to update.
         * {@link #EXTRA_DATA_XML_LAYOUT} specifies the layout of the list item.
         * {@link #EXTRA_LAYOUT_DATA} can be used to update views in the list
         * item layout with new values.
         * </p>
         * <p>
         * This intent should be sent with enforced security by supplying the
         * host application permission to sendBroadcast(Intent, String).
         * {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_DATA_XML_LAYOUT}</li>
         * <li>{@link #EXTRA_LIST_ITEM_ID}</li>
         * <li>{@link #EXTRA_LIST_ITEM_POSITION}</li>
         * <li>{@link #EXTRA_LAYOUT_DATA} (optional)</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_ITEM_INTENT = "com.sonyericsson.extras.aef.control.LIST_ITEM";

        /**
         * Intent sent by the Host Application to the Control extension when a
         * list item has been clicked. If the list item contains any views where
         * android:clickable is true and one of these views were clicked the
         * android:id of that view is returned in
         * {@link #EXTRA_LIST_ITEM_LAYOUT_REFERENCE}.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_LIST_ITEM_ID}</li>
         * <li>{@link #EXTRA_LIST_ITEM_POSITION}</li>
         * <li>{@link #EXTRA_CLICK_TYPE}</li>
         * <li>{@link #EXTRA_LIST_ITEM_LAYOUT_REFERENCE} (optional)</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_ITEM_CLICK_INTENT = "com.sonyericsson.extras.aef.control.LIST_ITEM_CLICK";

        /**
         * Intent sent by the Host Application to the Control extension when a
         * list item is selected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_LIST_ITEM_ID}</li>
         * <li>{@link #EXTRA_LIST_ITEM_POSITION}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_ITEM_SELECTED_INTENT = "com.sonyericsson.extras.aef.control.LIST_ITEM_SELECTED";

        /**
         * Intent sent by the Host Application to the Control extension when a
         * list refresh request is detected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_LIST_REFRESH_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.LIST_REFERESH_REQUEST";

        /**
         * Intent sent by the Control extension when it wants to show a menu.
         * The {@link #CONTROL_MENU_ITEM_SELECTED} intent is sent to the Control extension when a
         * menu item has been selected.
         *
         * <p>
         * This intent should be sent with enforced security by supplying the
         * host application permission to sendBroadcast(Intent, String).
         * {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_MENU_ITEMS}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_MENU_SHOW = "com.sonyericsson.extras.aef.control.MENU_SHOW";

        /**
         * Intent sent by the Host Application to the Control extension when a menu item has been
         * selected.
         * {@link #EXTRA_MENU_ITEM_ID} identifies the selected menu item.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * <li>{@link #EXTRA_MENU_ITEM_ID}</li>
         * </ul>
         * </p>
         *
         * @since 2.0
         */
        static final String CONTROL_MENU_ITEM_SELECTED = "com.sonyericsson.extras.aef.control.MENU_ITEM_SELECTED";

        /**
         * The name of the Intent-extra used to identify the Host Application.
         * The Host Application will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTRA_AHA_PACKAGE_NAME = "aha_package_name";

        /**
         * The name of the Intent-extra used to identify the Extension.
         * The Extension will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTRA_AEA_PACKAGE_NAME = "aea_package_name";

        /**
         * The name of the Intent-extra carrying the state of the display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #SCREEN_STATE_OFF}</li>
         * <li>{@link #SCREEN_STATE_DIM}</li>
         * <li>{@link #SCREEN_STATE_ON}</li>
         * <li>{@link #SCREEN_STATE_AUTO}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        static final String EXTRA_SCREEN_STATE = "screen_state";

        /**
         * The name of the Intent-extra carrying the ID of the LED to be controlled
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_LED_ID = "led_id";

        /**
         * The name of the Intent-extra carrying the color you want the LED to blink with
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_LED_COLOR = "led_color";

        /**
         * The name of the Intent-extra carrying the "on" duration in milliseconds
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_ON_DURATION = "on_duration";

        /**
         * The name of the Intent-extra carrying the "off" duration in milliseconds
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_OFF_DURATION = "off_duration";

        /**
         * The name of the Intent-extra carrying the number of repeats of the on/off pattern.
         * Note, the value {@link #REPEAT_UNTIL_STOP_INTENT} means that the on/off pattern is repeated until
         * the {@link #CONTROL_STOP_VIBRATE_INTENT} or {@link #CONTROL_STOP_LED_INTENT} intent is received
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_REPEATS = "repeats";

        /**
         * The name of the Intent-extra used to identify the URI of the image to be displayed on the
         * accessory display. If the image is in raw data (e.g. an array of bytes) use
         * {@link #EXTRA_DATA} instead
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTRA_DATA_URI = "data_uri";

        /**
         * The name of the Intent-extra used to identify the data to be displayed on the accessory
         * display. This Intent-extra should be used if the image is in raw data (e.g. an array of bytes)
         * <P>
         * TYPE: BYTE ARRAY
         * </P>
         * @since 1.0
         */
        static final String EXTRA_DATA = "data";

        /**
         * The name of the Intent-extra used to identify the pixel offset from the left side of the accessory
         * display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_X_OFFSET = "x_offset";

        /**
         * The name of the Intent-extra used to identify the pixel offset from the top of the accessory
         * display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_Y_OFFSET = "y_offset";

        /**
         * The name of the Intent-extra used to identify the type of key event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #KEY_ACTION_PRESS}</li>
         * <li>{@link #KEY_ACTION_RELEASE}</li>
         * <li>{@link #KEY_ACTION_REPEAT}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        static final String EXTRA_KEY_ACTION = "event_type";

        /**
         * The name of the Intent-extra used to carry the time stamp of the key or touch event
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_TIMESTAMP = "timestamp";

        /**
         * The name of the Intent-extra used to identify the keycode.
         * Information about what type of keypad a accessory has can be found using the
         * Registration &amp; Capabilities API
         * <P>
         * ALLOWED VALUES:
         * Any key code defined in the {@link KeyCodes} interface.
         * </P>
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_KEY_CODE = "key_code";

        /**
         * The name of the Intent-extra used to indicate the touch action
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #TOUCH_ACTION_PRESS}</li>
         * <li>{@link #TOUCH_ACTION_LONGPRESS}</li>
         * <li>{@link #TOUCH_ACTION_RELEASE}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        static final String EXTRA_TOUCH_ACTION = "action";

        /**
         * The name of the Intent-extra used to indicate the direction
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #SWIPE_DIRECTION_UP}</li>
         * <li>{@link #SWIPE_DIRECTION_DOWN}</li>
         * <li>{@link #SWIPE_DIRECTION_LEFT}</li>
         * <li>{@link #SWIPE_DIRECTION_RIGHT}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        static final String EXTRA_SWIPE_DIRECTION = "direction";

        /**
         * The name of the Intent-extra used to carry the X coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_X_POS = "x_pos";

        /**
         * The name of the Intent-extra used to carry the Y coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_Y_POS = "y_pos";

        /**
         * The name of the Intent-extra used to carry the error code
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>0: 'Registration information missing'</li>
         * <li>1: 'Accessory not connected'</li>
         * <li>2: 'Host Application busy'</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        static final String EXTRA_ERROR_CODE = "error_code";

        /**
         * The name of the Intent-extra containing the key set by the extension.
         * This Intent-data is present in all Intents sent by accessory host application,
         * except where {@link android.app.Activity#startActivity(android.content.Intent)}
         * is used. See section <a href="Registration.html#Security">Security</a>
         * for more information
         *
         * @since 1.0
         */
        static final String EXTRA_EXTENSION_KEY = "extension_key";

        /**
         * The name of the Intent-extra used to identify the data XML layout to be processed by the host application
         * and displayed by the accessory.
         * The layout resource id is used to identify the layout.
         * <P>
         * This is a standard Android layout, where a subset of the Android views are supported.
         * </P>
         * <h3>Dimensions</h3>
         * <P>
         * The px dimensions is the only dimension supported.
         * The only exception is text sizes which can be specified using sp to indicate that the text
         * shall be scaled to the user preference setting on the accessory.
         * </P>
         * <h3>ViewGroups</h3>
         * <P>
         * The following ViewGroups are supported:
         * <ul>
         * <li>AbsoluteLayout</li>
         * <li>FrameLayout</li>
         * <li>LinearLayout</li>
         * <li>RelativeLayout</li>
         * </ul>
         * All XML attributes are supported for the supported ViewGroups.
         * </P>
         * <h3>Views</h3>
         * <P>
         * The following Views are supported:
         * <ul>
         * <li>View</li>
         * <li>ImageView</li>
         * <li>TextView</li>
         * <li>ListView</li>
         * <li>Gallery</li>
         * </ul>
         * An accessory may support only a subset of these layouts.
         * {@link DeviceColumns#LAYOUT_SUPPORT} specifies which Views that are
         * supported for a certain accessory.
         * </p>
         * <p>
         * The following View XML attributes are supported
         * <ul>
         * <li>android:background - restricted to a solid color such as "#ff000000" (black)</li>
         * <li>android:clickable - {@link #CONTROL_OBJECT_CLICK_EVENT_INTENT} are sent for views that are clickable</li>
         * <li>android:id</li>
         * <li>android:layout_height</li>
         * <li>android:layout_width</li>
         * <li>android:padding</li>
         * <li>android:paddingBottom</li>
         * <li>android:paddingLeft</li>
         * <li>android:paddingRight</li>
         * <li>android:paddingTop</li>
         * </ul>
         * </P>
         * <h3>ImageView</h3>
         * <P>
         * For an ImageView the following XML attributes are supported
         * <ul>
         * <li>android:src - can be a BitmapDrawable or a NinePatchDrawable</li>
         * <li>android:scaleType</li>
         * </ul>
         * </P>
         * <h3>TextView</h3>
         * <P>
         * For a TextView the following XML attributes are supported
         * <ul>
         * <li>android:ellipsize - can be none, start, middle or end</li>
         * <li>android:gravity</li>
         * <li>android:lines</li>
         * <li>android:maxLines</li>
         * <li>android:singleLine</li>
         * <li>android:text</li>
         * <li>android:textColor</li>
         * <li>android:textSize - Not all text sizes are supported by all accessories.
         * If a not supported text size is used the accessory will select the closest available text size.
         * See the accessory white paper for a list of supported text sizes.
         * </br>
         * If the sp unit is used the text is scaled according to settings on the accessories (if supported by the accessory).
         * If the px unit is used the text is not affected by any settings on the accessory.
         * </li>
         * <li>android:textStyle</li>
         * </ul>
         * </P>
         * <h3>ListView and Gallery</h3>
         * <p>
         * For a ListView and a Gallery there are some additional limitations.
         * These views always have to fill the entire display width.
         * The items in these views also have to fill the entire display width.
         * The height of an item may not be larger than the height of the parent view.
         * </p>
         * <P>
         * TYPE: INTEGER
         * </P>
         * @since 2.0
         */
        static final String EXTRA_DATA_XML_LAYOUT = "data_xml_layout";

        /**
         * The name of the Intent-extra used to identify a reference within a layout.
         * Corresponds to the android:id XML attribute in the layout.
         * <P>
         * TYPE: INTEGER
         * </P>
         * @since 2.0
         */
        static final String EXTRA_LAYOUT_REFERENCE = "layout_reference";

        /**
         * The name of the Intent-extra used when sending a text (String)
         * from the extension to the accessory. The accessory will map the text
         * to a layout reference.
         * <P>
         * TYPE: STRING
         * </P>
         * @since 2.0
         */
        static final String EXTRA_TEXT = "text_from extension";

        /**
         * The name of the Intent-extra used for indicating the status of the Active Power State Mode.
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li> ACTIVE_POWER_SAVE_MODE_OFF</li>
         * <li> ACTIVE_POWER_SAVE_MODE_ON</li>
         * </ul>
         * </P>
         * @since 2.0
         */
        static final String EXTRA_ACTIVE_POWER_MODE_STATUS = "active_power_mode_status";

        /**
         * Data used to populate the views in a XML layout with dynamic info.
         * For example updating a TextView with a new text or setting a new
         * image in an ImageView. {@link #EXTRA_LAYOUT_REFERENCE} specifies the
         * view to be updated and one of {@link #EXTRA_TEXT},
         * {@link #EXTRA_DATA_URI} and {@link #EXTRA_DATA} specifies the new
         * information in the view.
         * <P>
         * TYPE: Array of BUNDLEs with following information in each BUNDLE.
         * <ul>
         * <li>{@link #EXTRA_LAYOUT_REFERENCE}</li>
         * <li>{@link #EXTRA_TEXT} or {@link #EXTRA_DATA_URI} or {@link #EXTRA_DATA}</li>
         * </ul>
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_LAYOUT_DATA = "layout_data";

        /**
         * Data to populate a ListView with initial content.
         *
         * <P>
         * TYPE: Array of BUNDLEs with following information in each BUNDLE.
         * <ul>
         * <li>{@link #EXTRA_DATA_XML_LAYOUT}</li>
         * <li>{@link #EXTRA_LIST_ITEM_ID}</li>
         * <li>{@link #EXTRA_LIST_ITEM_POSITION}</li>
         * <li>{@link #EXTRA_LAYOUT_DATA} (optional)</li>
         * </ul>
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_LIST_CONTENT = "list_content";

        /**
         * The type of a click.
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #CLICK_TYPE_SHORT}</li>
         * <li>{@link #CLICK_TYPE_LONG}</li>
         * </ul>
         * </P>
         * @since 2.0
         */
        static final String EXTRA_CLICK_TYPE = "click_type";

        /**
         * A unique identity of a list item assigned by the extension.
         * This can for example be the row id from a database.
         * <P>
         * TYPE: INTEGER
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_LIST_ITEM_ID = "list_item_id";

        /**
         * The position in a list. The position is in the range from 0 to the
         * {@link #EXTRA_LIST_COUNT}-1.
         * <P>
         * TYPE: INTEGER
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_LIST_ITEM_POSITION = "list_item_position";

        /**
         * The number of items in a list.
         * <P>
         * TYPE: INTEGER
         * </P>
         * @since 2.0
         */
        static final String EXTRA_LIST_COUNT = "list_count";

        /**
         * Reference to a view in a list item layout.
         * Corresponds to the android:id XML attribute in the layout.
         * <P>
         * TYPE: INTEGER
         * </P>
         * @since 2.0
         */
        static final String EXTRA_LIST_ITEM_LAYOUT_REFERENCE = "list_item_layout_reference";

        /**
         * If true then the user is allowed to initiate a refresh of the list
         * content. (For example by a pull to refresh gesture.)
         * <P>
         * TYPE: BOOLEAN
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_LIST_REFRESH_ALLOWED = "list_referesh_allowed";

        /**
         * The id of the display.
         * Refers to {@link Registration.DisplayColumns#_ID}.
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 2.0
         */
        static final String EXTRA_DISPLAY_ID = "displayId";

        /**
         * The URI of an icon (40x40 pixels) for a menu item.
         *
         * <P>
         * TYPE: TEXT
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_MENU_ITEM_ICON = "menuItemIcon";

        /**
         * The text for a menu item.
         *
         * <P>
         * TYPE: TEXT
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_MENU_ITEM_TEXT = "menuItemText";

        /**
         * A unique identity of a menu item assigned by the extension.
         * <P>
         * TYPE: INTEGER
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_MENU_ITEM_ID = "menuItemId";

        /**
         * Items for a menu. Each menu item can either be an icon or a text
         * string. The {@link #EXTRA_MENU_ITEM_ID} is used to identify the
         * selected menu item. {@link Registration.DisplayColumns#MENU_ITEMS}
         * specifies the number of menu items supported by the display.
         * <P>
         * TYPE: Array of BUNDLEs with following information in each BUNDLE.
         * <ul>
         * <li>{@link #EXTRA_MENU_ITEM_ID}</li>
         * <li>{@link #EXTRA_MENU_ITEM_ICON} or {@link #EXTRA_MENU_ITEM_TEXT}</li>
         * </ul>
         * </P>
         *
         * @since 2.0
         */
        static final String EXTRA_MENU_ITEMS = "menuItems";

        /**
         * The touch action is a press event.
         *
         * @since 1.0
         */
        static final int TOUCH_ACTION_PRESS = 0;

        /**
         * The touch action is a long press event
         *
         * @since 1.0
         */
        static final int TOUCH_ACTION_LONGPRESS = 1;

        /**
         * The touch action is a release event
         *
         * @since 1.0
         */
        static final int TOUCH_ACTION_RELEASE = 2;

        /**
         * The direction of the swipe event is up
         *
         * @since 1.0
         */
        static final int SWIPE_DIRECTION_UP = 0;

        /**
         * The direction of the swipe event is down
         *
         * @since 1.0
         */
        static final int SWIPE_DIRECTION_DOWN = 1;

        /**
         * The direction of the swipe event is left
         *
         * @since 1.0
         */
        static final int SWIPE_DIRECTION_LEFT = 2;

        /**
         * The direction of the swipe event is right
         *
         * @since 1.0
         */
        static final int SWIPE_DIRECTION_RIGHT = 3;

        /**
         * The screen off state
         *
         * @since 1.0
         */
        static final int SCREEN_STATE_OFF = 0;

        /**
         * The screen dim state
         *
         * @since 1.0
         */
        static final int SCREEN_STATE_DIM = 1;

        /**
         * The screen on state
         *
         * @since 1.0
         */
        static final int SCREEN_STATE_ON = 2;

        /**
         * The screen state is automatically handled by the host application
         *
         * @since 1.0
         */
        static final int SCREEN_STATE_AUTO = 3;

        /**
         * The key event is a key press event
         *
         * @since 1.0
         */
        static final int KEY_ACTION_PRESS = 0;

        /**
         * The key event is a key release event
         *
         * @since 1.0
         */
        static final int KEY_ACTION_RELEASE = 1;

        /**
         * The key event is a key repeat event
         *
         * @since 1.0
         */
        static final int KEY_ACTION_REPEAT = 2;

        /**
         * The control action is turned on
         *
         * @since 1.0
         */
        static final int CONTROL_ACTION_ON = 0;

        /**
         * The control action is turned off
         *
         * @since 1.0
         */
        static final int CONTROL_ACTION_OFF = 1;

        /**
         * Vibration or LED is repeated until explicitly stopped
         *
         * @since 1.0
         */
        static final int REPEAT_UNTIL_STOP_INTENT = -1;

        /**
         * Constant defining active power safe mode OFF.
         *
         * @since 2.0
         */
        static final int ACTIVE_POWER_SAVE_MODE_OFF = 0;

        /**
         * Constant defining active power safe mode ON.
         *
         * @since 2.0
         */
        static final int ACTIVE_POWER_SAVE_MODE_ON = 1;

        /**
         * A click is a short click.
         *
         * @since 2.0
         */
        static final int CLICK_TYPE_SHORT = 0;

        /**
         * The click is a long click.
         *
         * @since 2.0
         */
        static final int CLICK_TYPE_LONG = 1;
    }

    /**
     * Interface used to define constants for
     * keycodes
     */
    public interface KeyCodes {

        /**
         * Keycode representing a play button
         */
        static final int KEYCODE_PLAY = 1;

        /**
         * Keycode representing a next button
         */
        static final int KEYCODE_NEXT = 2;

        /**
         * Keycode representing a previous button
         */
        static final int KEYCODE_PREVIOUS = 3;

        /**
         * Keycode representing an action button
         */
        static final int KEYCODE_ACTION = 4;

        /**
         * Keycode representing a volume down button
         */
        static final int KEYCODE_VOLUME_DOWN = 5;

        /**
         * Keycode representing a volume up button
         */
        static final int KEYCODE_VOLUME_UP = 6;

        /**
         * Keycode representing a back button
         */
        static final int KEYCODE_BACK = 7;

        /**
         * Keycode representing an options button
         */
        static final int KEYCODE_OPTIONS = 8;
    }
}
