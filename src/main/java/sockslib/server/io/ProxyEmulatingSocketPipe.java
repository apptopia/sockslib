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
import sockslib.common.AnonymousCredentials;
import sockslib.common.Credentials;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyEmulatingSocketPipe extends SocketPipe {
  protected static final Logger logger = LoggerFactory.getLogger(ProxyEmulatingSocketPipe.class);

  private Pattern requestLinePattern =
      Pattern.compile("^(?<method>GET|HEAD|POST|PUT|DELETE|TRACE) (?<uri>/.*) HTTP/(?<version>\\d.\\d)$", Pattern.MULTILINE);

  private Pattern hostHeaderPattern = Pattern.compile("^Host: (?<host>.+)$", Pattern.MULTILINE);
  private Credentials credentials = new AnonymousCredentials();

  public ProxyEmulatingSocketPipe(Socket socket1, Socket socket2, Credentials credentials) throws IOException {
    super(socket1, socket2);

    this.setCredentials(credentials);
    this.pipe1.addPipeListener(new ProxyEmulatingPipeListener());
  }

  private void setCredentials(Credentials credentials) {
    this.credentials = credentials;
  }

  private Credentials getCredentials() {
    return credentials;
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
          String proxyAuthorization;
          if (getCredentials() instanceof AnonymousCredentials) {
            proxyAuthorization = "";
          } else {
            String credentialsPlain = getCredentials().getUserPrincipal().getName() + ":" + getCredentials().getPassword();
            String credentialsBase64 = Base64.getEncoder().encodeToString(credentialsPlain.getBytes());
            proxyAuthorization = "\nProxy-Authorization: Basic " + credentialsBase64;
          }

          String host = hostHeaderMatcher.group("host");
          String method = requestLineMatcher.group("method");
          String uri = requestLineMatcher.group("uri");
          String version = requestLineMatcher.group("version");
          String modifiedRequest =
                  requestLineMatcher.replaceFirst(method + " http://" + host + uri + " HTTP/" + version + proxyAuthorization);

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
