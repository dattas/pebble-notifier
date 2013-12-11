/*
Copyright (c) 2013 Sony Mobile Communications AB

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


public class ControlView {

    private int id;
    private boolean isClickable;
    private boolean isLongClickable;

    static class ListenerInfo {
        public OnClickListener mOnClickListener;
        public OnLongClickListener mOnLongClickListener;
    }

    ListenerInfo mListenerInfo;

    public ControlView(int id, boolean isClickable, boolean isLongClickable) {
        this.id = id;
        this.isClickable = isClickable;
        this.isLongClickable = isLongClickable;
    }

    public interface OnClickListener {
        void onClick();

    }

    public interface OnLongClickListener {
        void onLongClick();
    }

    public void setOnClickListener(OnClickListener l) {
        if (isClickable) {
            getListenerInfo().mOnClickListener = l;
        }
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        if (isLongClickable) {
            getListenerInfo().mOnLongClickListener = l;
        }
    }

    public void onClick() {
        if (isClickable && getListenerInfo().mOnClickListener != null) {
            getListenerInfo().mOnClickListener.onClick();
        }
    }

    public void onLongClick() {
        if (isLongClickable && getListenerInfo().mOnLongClickListener != null) {
            getListenerInfo().mOnLongClickListener.onLongClick();
        }
    }

    public int getId() {
        return id;
    }

    private ListenerInfo getListenerInfo() {
        if (mListenerInfo != null) {
            return mListenerInfo;
        }
        mListenerInfo = new ListenerInfo();
        return mListenerInfo;
    }

}
