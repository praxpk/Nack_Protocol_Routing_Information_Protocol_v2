import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;


public class DataClient extends Thread {
    private ConcurrentHashMap<String, byte[]> packet_store = new ConcurrentHashMap<>();
    DatagramSocket ds1;
    private int current_packect_count;
    private int start_index;
    private int end_index;
    private boolean last_packet_received;
    private InetAddress sender_ip;
    private int sender_port;
    private volatile boolean syn_ack;
    private volatile boolean ack_received;
    private static OutputStream output;
    private int session_id;
    private InetAddress this_ip;
    Routing_table rt;

    public DataClient(DatagramSocket ds1, InetAddress assigned_ip, Routing_table rt, OutputStream output) {
        this.rt = rt;
        this.ds1 = ds1;
        this.current_packect_count = 0;
        last_packet_received = false;
        this.output = output;
        this_ip = assigned_ip;
        syn_ack = false;
        ack_received = false;
    }

    public void reset() {
        packet_store = new ConcurrentHashMap<>();
        current_packect_count = 0;
        start_index = 0;
        end_index = 0;
        last_packet_received = false;
        syn_ack = false;
        ack_received = false;
        session_id = 0;
    }

    public InetAddress obtain_real_ip(InetAddress dest) {
        InetAddress result_ip = rt.getRoverEntry(dest).getVia_router();
        return result_ip;
    }

    public void setSender_ip(InetAddress sender_ip) {
        this.sender_ip = sender_ip;
    }

    public InetAddress getSender_ip() {
        return sender_ip;
    }

    public void setSender_port(int sender_port) {
        this.sender_port = sender_port;
    }

    public boolean isLast_packet_received() {
        return last_packet_received;
    }

    public int getSender_port() {
        return sender_port;
    }


    public static byte[] intToByte(int num) {
        byte[] b1 = new byte[2];

        ByteBuffer.wrap(b1).putShort((short) num);

        return b1;
    }

    public static int byte_to_int(byte[] data) {
        int num = ByteBuffer.wrap(data).getShort();
        return num;
    }


    public void update_start_end(int start, int end) {
        start_index = start;
        end_index = end;
    }


    public void process_data(byte[] packet)
    /**
     * id is the three tuple startsegment#endsegment#segmentnumber
     */
    {
        byte[] temp_bytes = new byte[2];
        System.arraycopy(packet, 12, temp_bytes, 0, 2);
        int start_seg = byte_to_int(temp_bytes);
        System.arraycopy(packet, 14, temp_bytes, 0, 2);
        int end_seg = byte_to_int(temp_bytes);
        System.arraycopy(packet, 16, temp_bytes, 0, 2);
        int seg = byte_to_int(temp_bytes);
        // to check if the start and end segment have changed.
        update_start_end(start_seg, end_seg);

        if (seg == end_seg) {
            last_packet_received = true;
            System.out.println("last packet received");
        }

        String id = (start_seg) + "#" + (end_seg) + "#" + (seg);
        if (!packet_store.containsKey(id)) {
            byte[] data = new byte[packet.length - 18];
            System.arraycopy(packet, 18, data, 0, packet.length - 18);

            packet_store.put(id, data);
            current_packect_count++; //keeps a current_packect_count of the unique packets, where each segment is
            // identified by tuple (start segment, end segment, segment number),
            //that have been added to the hashmap.
        }

    }

    public byte[] create_nack(byte[] packet, int missing_seg) {
        packet[0] = set_bit_at(packet[0], 5); //NACK set is 0010 0000
        byte[] missing_segment = intToByte(missing_seg);
        packet = add_ip_address(packet, sender_ip, this_ip);
        packet = add_session_number(packet, session_id);
        packet[12] = missing_segment[0];
        packet[13] = missing_segment[1];
        return packet;
    }

    public byte[] add_session_number(byte[] packet, int session) {
        byte[] temp = intToByte(session);
        System.arraycopy(temp, 0, packet, 10, 2);
        return packet;
    }

    public byte[] add_ip_address(byte[] packet, InetAddress dest, InetAddress source) {
        byte[] temp;
        temp = dest.getAddress();
        System.arraycopy(temp, 0, packet, 2, 4);
        temp = source.getAddress();
        System.arraycopy(temp, 0, packet, 6, 4);
        return packet;
    }

    public byte[] create_all_received(byte[] packet) {
        packet[0] = set_bit_at(packet[0], 4); //ALL_RCVD set is 0000 1000
        packet = add_ip_address(packet, sender_ip, this_ip);
        packet = add_session_number(packet, session_id);
        return packet;
    }

    public byte[] create_syn_ack(byte[] packet) {
        try {
            packet[0] = set_bit_at(packet[0], 4); //ACK set is 0001 0000
            packet[0] = set_bit_at(packet[0], 7); //SYN set is 1000 0000
            packet[1] = intToByte(2)[1];
            packet = add_ip_address(packet, sender_ip, this_ip);
            byte[] temp;
            temp = intToByte(session_id);
            System.arraycopy(temp, 0, packet, 10, 2);
            temp = intToByte(0);//setting self start segment =0 as no data sent
            System.arraycopy(temp, 0, packet, 12, 2);
            System.arraycopy(temp, 0, packet, 14, 2);
            temp = intToByte(start_index);
            System.arraycopy(temp, 0, packet, 16, 2);
            temp = intToByte(end_index);
            System.arraycopy(temp, 0, packet, 18, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packet;
    }

    public byte set_bit_at(byte packet, int pos) {
        return (byte) ((packet | (1 << pos)));
    }

    public boolean isSet(byte b, int pos) {
        boolean result;
        if ((b & (1 << pos)) != 0) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public void process_syn(byte[] packet) {
        byte[] temp = new byte[2];
        System.arraycopy(packet, 12, temp, 0, 2);
        start_index = byte_to_int(temp);
        System.arraycopy(packet, 14, temp, 0, 2);
        end_index = byte_to_int(temp);
        System.arraycopy(packet, 10, temp, 0, 2);
        this.session_id = byte_to_int(temp);
        syn_ack = true;

    }

    public void process_ack() {
        ack_received = true;
    }

    public void deposit_packet(byte[] packet) {
        byte[] temp = new byte[2];
        if (isSet(packet[0], 7) && !syn_ack) {
            System.arraycopy(packet, 10, temp, 0, 2);
            session_id = byte_to_int(temp);
            process_syn(packet);
            syn_ack = true;
        } else if (isSet(packet[0], 6)) {
            System.arraycopy(packet, 10, temp, 0, 2);
            if (byte_to_int(temp) == session_id) {
                process_data(packet);
            }
        } else if (isSet(packet[0], 4)) {
            System.arraycopy(packet, 10, temp, 0, 2);
            System.out.println("ack, session id rcv= " + byte_to_int(temp) + "session id rec = " + session_id);
            if (byte_to_int(temp) == session_id) {
                process_ack();
            }
        }
    }


    public void threeway_handshake() {
        //waiting for client to receive syn packet
        try {
            long time1 = System.currentTimeMillis();
            long time2;
            while (!syn_ack) {
                time2 = System.currentTimeMillis();
                if (time2 - time1 > 30000) {
                    System.out.println("Timeout: Server Unavailable");
                }
            }
            System.out.println("SYN received");
            byte[] packet = new byte[25];
            packet = create_syn_ack(packet);
            System.out.println("SYN-ACK sent");

            time1 = System.currentTimeMillis();
            while (!ack_received) {
                InetAddress dest = obtain_real_ip(sender_ip);
                new Sender(packet, dest, 60000, ds1).start();

                time2 = System.currentTimeMillis();
                Thread.sleep(100);
                if (time2 - time1 > 30000) {
                    System.out.println("Timeout: Server Unavailable");
                    return;
                }
            }
            System.out.println("ACK received");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void assemble_packet() {
        //if all the packets from start index to end index are present in packet_Store (while loop)
        //remove the packets from packet store and assemble them in byte array
        //call method to write byte array to file.
        try {
            while ((last_packet_received) && (current_packect_count != end_index + 1)) {
                byte[] nack_packet;
                for (int i = 0; i < current_packect_count; i++) {
                    String id = "" + start_index + "#" + end_index + "#" + i;
                    if (!packet_store.containsKey(id)) {
                        nack_packet = create_nack(new byte[25], i);
                        System.out.println("sending nack for segment = " + i);
                        InetAddress dest = obtain_real_ip(sender_ip);
                        Thread.sleep(1);
                        new Sender(nack_packet, dest, 60000, ds1).start();
                    }

                }
                Thread.sleep(500);
            }
            byte[] all_rcvd = new byte[25];
            all_rcvd = create_all_received(all_rcvd);
            InetAddress dest = obtain_real_ip(sender_ip);
            new Sender(all_rcvd, dest, 60000, ds1).start();


            System.out.println("All packets of check received (sessio id)" + session_id);
            byte[] temp_data;
            String key = "";
            for (int i = start_index; i <= end_index; i = i + 1) {
                key = start_index + "#" + end_index + "#" + i;
                if (packet_store.containsKey(key)) {
                    temp_data = packet_store.get(key);
                    packet_store.remove(key);
                    output.write(temp_data);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("Done writing stream to file");

    }

    public void run() {
        try {
            System.out.println("starting client");
            threeway_handshake();
            System.out.println("=========== Current Session Id " + session_id + " ==========");
            while (!last_packet_received) {
                Thread.sleep(3000);
                System.out.println(packet_store.size() + " packets received from client");
            }
            assemble_packet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
