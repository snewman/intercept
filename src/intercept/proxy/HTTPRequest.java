package intercept.proxy;

import intercept.utils.Block;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public class HTTPRequest {
    public byte[] requestData;
    String host = "";
    public int contentLength = 0;
    String path;
    private byte[] headerData;
    private int originalHeaderSize;

    public HTTPRequest() {
        path = "";
    }

    public String hostName() {
        int pos = host.indexOf(":");
        if (pos > 0) {
            return host.substring(0, pos);
        }
        return host;
    }

    public String getPath() {
        return path;
    }

    public int hostPortNumber() {
        return this.hostPortNumber(80);
    }

    public int hostPortNumber(int defaultPort) {
        int pos = host.indexOf(":");
        if (pos > 0) {
            try {
                return Integer.parseInt(host.substring(pos + 1));
            } catch (Exception e) {
                throw new RuntimeException("Malformed port number");
            }
        }
        return defaultPort;
    }

    public void replaceHost(String address) {
        String r = new String(requestData);

        r = r.replaceAll(hostName(), address);

        host = address;
        requestData = r.getBytes();
    }

    public int length() {
        return requestData == null ? 0 : Array.getLength(requestData);
    }

    public void write(BufferedOutputStream outputStream) throws IOException {
        outputStream.write(headerData, 0, headerData.length);
        outputStream.write(requestData, originalHeaderSize, length() - originalHeaderSize);
    }

    public void updateHost(String data) {
        if (data.toLowerCase().startsWith("host:")) {
            host = data.substring("host:".length()).trim();
        }
    }

    private void parseContentLength(String data, Block<Integer> block) {
        int pos = data.toLowerCase().indexOf("content-length:");
        if (pos >= 0) {
            block.yield(Integer.parseInt(data.substring(pos + 15).trim()));
        }
    }

    public void updateContentLength(String data) {
        parseContentLength(data, new Block<Integer>() {
            public void yield(Integer item) {
                contentLength = Math.max(contentLength, item);
            }
        });
    }

    public void updatePath(String data) {
        int pos = data.toLowerCase().indexOf("get");
        if (pos == 0) {
            int httpPos = data.toLowerCase().lastIndexOf("http");
            if (httpPos >= 0) {
                path = data.substring(4, httpPos).trim();
            } else {
                path = data.substring(4).trim();
            }
        }
    }

    public void addConnectionCloseHeader() {
        String modifiedHeader = new String(headerData) + "Connection: close\r\n";
        headerData = modifiedHeader.getBytes();
    }

    public void setHeaderData(byte[] headerData) {
        this.headerData = headerData;
        originalHeaderSize = headerData.length;
    }
}
