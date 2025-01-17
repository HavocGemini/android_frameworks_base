/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.am;

import android.app.IServiceConnection;
import android.app.PendingIntent;
import android.content.Context;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * Description of a single binding to a service.
 */
final class ConnectionRecord {
    final AppBindRecord binding;    // The application/service binding.
    final ActivityRecord activity;  // If non-null, the owning activity.
    final IServiceConnection conn;  // The client connection.
    final int flags;                // Binding options.
    final int clientLabel;          // String resource labeling this client.
    final PendingIntent clientIntent; // How to launch the client.
    String stringName;              // Caching of toString.
    boolean serviceDead;            // Well is it?

    // Please keep the following two enum list synced.
    private static int[] BIND_ORIG_ENUMS = new int[] {
            Context.BIND_AUTO_CREATE,
            Context.BIND_DEBUG_UNBIND,
            Context.BIND_NOT_FOREGROUND,
            Context.BIND_IMPORTANT_BACKGROUND,
            Context.BIND_ABOVE_CLIENT,
            Context.BIND_ALLOW_OOM_MANAGEMENT,
            Context.BIND_WAIVE_PRIORITY,
            Context.BIND_IMPORTANT,
            Context.BIND_ADJUST_WITH_ACTIVITY,
            Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
            Context.BIND_FOREGROUND_SERVICE,
            Context.BIND_TREAT_LIKE_ACTIVITY,
            Context.BIND_VISIBLE,
            Context.BIND_SHOWING_UI,
            Context.BIND_NOT_VISIBLE,
    };
    private static int[] BIND_PROTO_ENUMS = new int[] {
            ConnectionRecordProto.AUTO_CREATE,
            ConnectionRecordProto.DEBUG_UNBIND,
            ConnectionRecordProto.NOT_FG,
            ConnectionRecordProto.IMPORTANT_BG,
            ConnectionRecordProto.ABOVE_CLIENT,
            ConnectionRecordProto.ALLOW_OOM_MANAGEMENT,
            ConnectionRecordProto.WAIVE_PRIORITY,
            ConnectionRecordProto.IMPORTANT,
            ConnectionRecordProto.ADJUST_WITH_ACTIVITY,
            ConnectionRecordProto.FG_SERVICE_WHILE_AWAKE,
            ConnectionRecordProto.FG_SERVICE,
            ConnectionRecordProto.TREAT_LIKE_ACTIVITY,
            ConnectionRecordProto.VISIBLE,
            ConnectionRecordProto.SHOWING_UI,
            ConnectionRecordProto.NOT_VISIBLE,
    };

    void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "binding=" + binding);
        if (activity != null) {
            pw.println(prefix + "activity=" + activity);
        }
        pw.println(prefix + "conn=" + conn.asBinder()
                + " flags=0x" + Integer.toHexString(flags));
    }

    ConnectionRecord(AppBindRecord _binding, ActivityRecord _activity,
               IServiceConnection _conn, int _flags,
               int _clientLabel, PendingIntent _clientIntent) {
        binding = _binding;
        activity = _activity;
        conn = _conn;
        flags = _flags;
        clientLabel = _clientLabel;
        clientIntent = _clientIntent;
    }

    public String toString() {
        if (stringName != null) {
            return stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ConnectionRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(binding.client.userId);
        sb.append(' ');
        if ((flags&Context.BIND_AUTO_CREATE) != 0) {
            sb.append("CR ");
        }
        if ((flags&Context.BIND_DEBUG_UNBIND) != 0) {
            sb.append("DBG ");
        }
        if ((flags&Context.BIND_NOT_FOREGROUND) != 0) {
            sb.append("!FG ");
        }
        if ((flags&Context.BIND_IMPORTANT_BACKGROUND) != 0) {
            sb.append("IMPB ");
        }
        if ((flags&Context.BIND_ABOVE_CLIENT) != 0) {
            sb.append("ABCLT ");
        }
        if ((flags&Context.BIND_ALLOW_OOM_MANAGEMENT) != 0) {
            sb.append("OOM ");
        }
        if ((flags&Context.BIND_WAIVE_PRIORITY) != 0) {
            sb.append("WPRI ");
        }
        if ((flags&Context.BIND_IMPORTANT) != 0) {
            sb.append("IMP ");
        }
        if ((flags&Context.BIND_ADJUST_WITH_ACTIVITY) != 0) {
            sb.append("WACT ");
        }
        if ((flags&Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE) != 0) {
            sb.append("FGSA ");
        }
        if ((flags&Context.BIND_FOREGROUND_SERVICE) != 0) {
            sb.append("FGS ");
        }
        if ((flags&Context.BIND_TREAT_LIKE_ACTIVITY) != 0) {
            sb.append("LACT ");
        }
        if ((flags&Context.BIND_VISIBLE) != 0) {
            sb.append("VIS ");
        }
        if ((flags&Context.BIND_SHOWING_UI) != 0) {
            sb.append("UI ");
        }
        if ((flags&Context.BIND_NOT_VISIBLE) != 0) {
            sb.append("!VIS ");
        }
        if (serviceDead) {
            sb.append("DEAD ");
        }
        sb.append(binding.service.shortName);
        sb.append(":@");
        sb.append(Integer.toHexString(System.identityHashCode(conn.asBinder())));
        sb.append('}');
        return stringName = sb.toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        if (binding == null) return; // if binding is null, don't write data, something is wrong.
        long token = proto.start(fieldId);
        proto.write(ConnectionRecordProto.HEX_HASH,
                Integer.toHexString(System.identityHashCode(this)));
        if (binding.client != null) {
            proto.write(ConnectionRecordProto.USER_ID, binding.client.userId);
        }
        ProtoUtils.writeBitWiseFlagsToProtoEnum(proto, ConnectionRecordProto.FLAGS,
                flags, BIND_ORIG_ENUMS, BIND_PROTO_ENUMS);
        if (serviceDead) {
            proto.write(ConnectionRecordProto.FLAGS, ConnectionRecordProto.DEAD);
        }
        if (binding.service != null) {
            proto.write(ConnectionRecordProto.SERVICE_NAME, binding.service.shortName);
        }
        proto.end(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionRecord that = (ConnectionRecord) o;
        return flags == that.flags
                && clientLabel == that.clientLabel
                && serviceDead == that.serviceDead
                && Objects.equals(binding, that.binding)
                && Objects.equals(activity, that.activity)
                && Objects.equals(conn, that.conn)
                && Objects.equals(clientIntent, that.clientIntent)
                && Objects.equals(stringName, that.stringName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(binding, activity, conn, flags, clientLabel, clientIntent
                , stringName, serviceDead);
    }
}
