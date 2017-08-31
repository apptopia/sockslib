/*
 * Copyright 2015-2025 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package sockslib.server.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyEmulatingSocketPipe extends SocketPipe {
  protected static final Logger logger = LoggerFactory.getLogger(ProxyEmulatingSocketPipe.class);

  private Pattern requestLinePattern =
      Pattern.compile("^(?<method>GET|HEAD|POST|PUT|DELETE|TRACE) (?<uri>/.*) HTTP/(?<version>\\d.\\d)$", Pattern.MULTILINE);

  private Pattern hostHeaderPattern = Pattern.compile("^Host: (?<host>.+)$", Pattern.MULTILINE);

  public ProxyEmulatingSocketPipe(Socket socket1, Socket socket2) throws IOException {
    super(socket1, socket2);

    this.pipe1.addPipeListener(new ProxyEmulatingPipeListener());
  }

  private class ProxyEmulatingPipeListener implements PipeListener {

    @Override
    public void onStart(Pipe pipe) {

    }

    @Override
    public void onStop(Pipe pipe) {

    }

    @Override
    public BufferAndLength onBeforeTransfer(Pipe pipe, byte[] buffer, int bufferLength) {
      Charset ascii = Charset.forName("US-ASCII");
      String request = new String(buffer, 0, bufferLength, ascii);

      Matcher hostHeaderMatcher = hostHeaderPattern.matcher(request);
      if (hostHeaderMatcher.find()) {
        Matcher requestLineMatcher = requestLinePattern.matcher(request);
        if (requestLineMatcher.find()) {
          String host = hostHeaderMatcher.group("host");
          String method = requestLineMatcher.group("method");
          String uri = requestLineMatcher.group("uri");
          String version = requestLineMatcher.group("version");
          String modifiedRequest =
                  requestLineMatcher.replaceFirst(method + " http://" + host + uri + " HTTP/" + version);

          byte[] modifiedRequestBuffer = modifiedRequest.getBytes(ascii);
          return new BufferAndLength(modifiedRequestBuffer, modifiedRequestBuffer.length);
        } else {
          return new BufferAndLength(buffer, bufferLength);
        }
      } else {
        return new BufferAndLength(buffer, bufferLength);
      }
    }

    @Override
    public void onTransfer(Pipe pipe, byte[] buffer, int bufferLength) {

    }

    @Override
    public void onError(Pipe pipe, Exception exception) {

    }

  }

}
