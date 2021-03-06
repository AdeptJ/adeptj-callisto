/*
###############################################################################
#                                                                             #
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/

package com.adeptj.runtime.websocket;

import org.apache.commons.io.input.TailerListenerAdapter;

import javax.websocket.Session;

/**
 * Simple implementation of a {@link org.apache.commons.io.input.TailerListener}.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
class ServerLogsTailerListener extends TailerListenerAdapter {

    private Session webSocketSession;

    void setWebSocketSession(Session webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    @Override
    public void handle(String line) {
        this.webSocketSession.getAsyncRemote().sendText(line);
    }

    @Override
    public void handle(Exception ex) {
        this.webSocketSession.getAsyncRemote().sendText(ex.toString());
    }
}
