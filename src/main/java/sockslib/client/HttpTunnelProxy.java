package sockslib.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockslib.common.AnonymousCredentials;
import sockslib.common.Credentials;
import sockslib.common.UsernamePasswordCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * The class <code>HttpTunnelProxy</code> implements HTTP tunnel protocol.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc2817.txt">Upgrading to TLS Within HTTP/1.1</a>
 */
public class HttpTunnelProxy {

    protected static final Logger logger = LoggerFactory.getLogger(HttpTunnelProxy.class);
    private HttpTunnelProxy chainProxy;
    /**
     * Authentication.
     */
    private Credentials credentials = new AnonymousCredentials();
    /**
     * HTTP server's address. IPv4 or IPv6 address.
     */
    private InetAddress inetAddress;

    private final int HTTP_DEFAULT_PORT = 80;

    private static final int SO_TIMEOUT_MS = 15000;

    /**
     * HTTP server's port;
     */
    private int port = HTTP_DEFAULT_PORT;

    /**
     * The socket that will connect to HTTP server.
     */
    private Socket proxySocket;

    private boolean plainHttpProxy = false;

    /**
     * Constructs a HttpTunnelProxy instance.
     *
     * @param socketAddress HTTP server's address.
     * @param username      Username of the authentication.
     * @param password      Password of the authentication.
     */
    public HttpTunnelProxy(SocketAddress socketAddress, String username, String password) {
        this(socketAddress);
        setCredentials(new UsernamePasswordCredentials(username, password));
    }

    /**
     * Constructs a HttpTunnelProxy instance.
     *
     * @param host HTTP server's host.
     * @param port HTTP server's port.
     * @throws UnknownHostException If the host can't be resolved.
     */
    public HttpTunnelProxy(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    /**
     * Constructs a HttpTunnelProxy instance.
     *
     * @param inetAddress HTTP server's address.
     * @param port        HTTP server's port.
     */
    public HttpTunnelProxy(InetAddress inetAddress, int port) {
        this(new InetSocketAddress(inetAddress, port));
    }

    /**
     * Constructs a HttpTunnelProxy instance with a java.net.SocketAddress instance.
     *
     * @param socketAddress HTTP server's address.
     */
    public HttpTunnelProxy(SocketAddress socketAddress) {
        this(null, socketAddress);
    }

    public HttpTunnelProxy(HttpTunnelProxy chainProxy, SocketAddress socketAddress) {
        init();
        if (socketAddress instanceof InetSocketAddress) {
            inetAddress = ((InetSocketAddress) socketAddress).getAddress();
            port = ((InetSocketAddress) socketAddress).getPort();
            this.setChainProxy(chainProxy);
        } else {
            throw new IllegalArgumentException("Only supports java.net.InetSocketAddress");
        }
    }

    /**
     * Constructs a HttpTunnelProxy instance.
     *
     * @param host        HTTP server's host.
     * @param port        HTTP server's port.
     * @param credentials credentials.
     * @throws UnknownHostException If the host can't be resolved.
     */
    public HttpTunnelProxy(String host, int port, Credentials credentials) throws UnknownHostException {
        init();
        this.inetAddress = InetAddress.getByName(host);
        this.port = port;
        this.credentials = credentials;
    }

    /**
     * Constructs a HttpTunnelProxy instance without any parameter.
     */
    private void init() {
    }

    public void buildConnection() throws IOException {
        if (inetAddress == null) {
            throw new IllegalArgumentException("Please set inetAddress before calling buildConnection.");
        }
        if (proxySocket == null) {
            proxySocket = createProxySocket(inetAddress, port);
        } else if (!proxySocket.isConnected()) {
            proxySocket.setSoTimeout(SO_TIMEOUT_MS);
            proxySocket.connect(new InetSocketAddress(inetAddress, port), SO_TIMEOUT_MS);
        }
    }

    public void requestConnect(String host, int port) throws IOException,
            IOException {
        doTunnelHandshake(proxySocket, host, port);
    }

    public void requestConnect(InetAddress address, int port) throws IOException,
            IOException {
        doTunnelHandshake(proxySocket, address.getHostName(), port);
    }

    public void requestConnect(SocketAddress socketAddress) throws IOException,
            IOException {
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        final InetSocketAddress address = (InetSocketAddress) socketAddress;
        doTunnelHandshake(proxySocket, address.getHostName(), address.getPort());
    }

    public int getPort() {
        return port;
    }

    public HttpTunnelProxy setPort(int port) {
        this.port = port;
        return this;
    }

    public Socket getProxySocket() {
        return proxySocket;
    }

    public boolean isPlainHttpProxy() {
        return plainHttpProxy;
    }

    public HttpTunnelProxy setProxySocket(Socket proxySocket) {
        this.proxySocket = proxySocket;
        return this;
    }

    public InputStream getInputStream() throws IOException {
        return proxySocket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return proxySocket.getOutputStream();
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public HttpTunnelProxy setCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public HttpTunnelProxy copy() {
        HttpTunnelProxy httpTunnelProxy = new HttpTunnelProxy(inetAddress, port);
        httpTunnelProxy.setCredentials(credentials).setChainProxy(chainProxy);
        return httpTunnelProxy;
    }

    public HttpTunnelProxy copyWithoutChainProxy() {
        return copy().setChainProxy(null);
    }

    public HttpTunnelProxy getChainProxy() {
        return chainProxy;
    }

    public HttpTunnelProxy setChainProxy(HttpTunnelProxy chainProxy) {
        this.chainProxy = chainProxy;
        return this;
    }

    public HttpTunnelProxy setHost(String host) throws UnknownHostException {
        inetAddress = InetAddress.getByName(host);
        return this;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    /**
     * Sets HTTP proxy server's IP address.
     *
     * @param inetAddress IP address of HTTP proxy server.
     * @return The instance of {@link HttpTunnelProxy}.
     */
    public HttpTunnelProxy setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder stringBuffer = new StringBuilder("[HTTP:");
        stringBuffer.append(new InetSocketAddress(inetAddress, port)).append("]");
        if (getChainProxy() != null) {
            return stringBuffer.append(" --> ").append(getChainProxy().toString()).toString();
        }
        return stringBuffer.toString();
    }

    public Socket createProxySocket(InetAddress address, int port) throws IOException {
        return new Socket(address, port);
    }

    public Socket createProxySocket() throws IOException {
        return new Socket();
    }

    private void doTunnelHandshake(Socket tunnel, String targetHost, int targetPort) throws IOException {
        if (isHttpPort(targetPort)) {
            // HTTP tunnel proxies don't like CONNECTs to plain-HTTP ports
            this.plainHttpProxy = true;
            return;
        }

        OutputStream out = tunnel.getOutputStream();
        String msg = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.0\n"
                + "Host: " + targetHost + "\n"
                + "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:53.0) Gecko/20100101 Firefox/53.0"
                + "\r\n\r\n";
        byte b[];
        try {
            b = msg.getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            b = msg.getBytes();
        }
        out.write(b);
        out.flush();

        byte reply[] = new byte[200];
        int replyLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false;

        InputStream in = tunnel.getInputStream();

        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from proxy");
            }
            if (i == '\n') {
                headerDone = true;
                ++newlinesSeen;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone && replyLen < reply.length) {
                    reply[replyLen++] = (byte) i;
                }
            }
        }

        String replyStr;
        try {
            replyStr = new String(reply, 0, replyLen, "ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            replyStr = new String(reply, 0, replyLen);
        }

        if (!replyStr.startsWith("HTTP/1.0 200") && !replyStr.startsWith("HTTP/1.1 200")) {
            throw new IOException("Unable to tunnel through "
                    + inetAddress.getHostAddress() + ":" + port
                    + ".  Proxy returns \"" + replyStr + "\"");
        }
    }

    private boolean isHttpPort(int port) {
        return port == 80 || port == 8008 || port == 8080;
    }
}
