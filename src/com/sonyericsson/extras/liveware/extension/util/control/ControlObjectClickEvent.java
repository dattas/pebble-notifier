/*
Copyright (c) 2013, Sony Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

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

package com.sonyericsson.extras.liveware.extension.util.control;

import com.sonyericsson.extras.liveware.aef.control.Control;

/**
 * The control object click event class holds information about an object click
 * event.
 */
public class ControlObjectClickEvent {
    private final int mClickType;
    private final long mTimeStamp;
    private final int mLayoutReference;

    /**
     * Create click event.
     * 
     * @see Control.Intents#CLICK_TYPE_SHORT
     * @see Control.Intents#CLICK_TYPE_LONG
     * @param clickType The click type.
     * @param timeStamp The time when the event occurred.
     * @param layoutReference Reference to view in the layout
     */
    public ControlObjectClickEvent(final int clickType, final long timeStamp,
            final int layoutReference) {
        mClickType = clickType;
        mTimeStamp = timeStamp;
        mLayoutReference = layoutReference;
    }

    /**
     * Get the click type.
     * 
     * @return The click type.
     */
    public int getClickType() {
        return mClickType;
    }

    /**
     * Get the touch event time stamp.
     * 
     * @return The time stamp.
     */
    public long getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Get the layout reference.
     * 
     * @return Reference to view in the layout.
     */
    public int getLayoutReference() {
        return mLayoutReference;
    }

}
