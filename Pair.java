import java.net.InetAddress;

public class Pair<A, B> {
    private InetAddress a;
    private byte [] b;

    public Pair(InetAddress a, byte [] b) {
        this.a = a;
        this.b = b;
    }

    public InetAddress getA() {
        return a;
    }

    public byte [] getB() {
        return b;
    }
}
