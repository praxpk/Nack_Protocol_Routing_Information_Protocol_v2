import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;


public class Routing_table extends Thread{
    private static ConcurrentHashMap<InetAddress, RoverEntry> route_table =
            new ConcurrentHashMap<>();
    //if an incoming update contains an ip address that does not
    // exist in route_table, write method to update table.
    private static BlockingQueue<Pair<InetAddress, byte[]>> incoming_packets =
            new LinkedBlockingQueue<>();
    private static InetAddress this_assigned_ip;
    private static InetAddress this_real_ip;
    private static boolean active_table;
    public static DatagramSocket sender_socket;

    public Routing_table(InetAddress this_assigned_ip,InetAddress this_real_ip, DatagramSocket ds){
        this.active_table = true;
        this.this_assigned_ip = this_assigned_ip;
        this.sender_socket = ds;
        this.this_real_ip = this_real_ip;

        //adding current router into table
        RoverEntry temp_rover = new RoverEntry(this.this_real_ip,
                this.this_assigned_ip,this.this_real_ip,this.this_assigned_ip,
                0,this,false);
        route_table.put(this.this_assigned_ip,temp_rover);
//        System.out.println("RT.java line 31: Routing table initialized");
    }
    public boolean containsRoverEntry(InetAddress rover_address){
        if(route_table.containsKey(rover_address)){
            return true;
        }
        return false;
    }
    public RoverEntry getRoverEntry(InetAddress rover_address){
        return route_table.get(rover_address);
    }

    public void addRoverEntry(InetAddress real_ip, InetAddress given_ip){
        RoverEntry temp_roverEntry =new RoverEntry(real_ip,
                given_ip,real_ip,this_assigned_ip,
                1,this,true);
        route_table.put(given_ip, temp_roverEntry);
        temp_roverEntry.start();
    }

    public InetAddress obtain_real_ip_address(InetAddress assigned_ip)
    /**
     * for a given assigned_ip address, we return its real ip address.
     */
    {
        RoverEntry temp_roverEntry = route_table.get(assigned_ip);
        return temp_roverEntry.getReal_router_address();
    }

    public void update_to_16(InetAddress i1)
    /**
     * this method obtains the ip addresses of all rovers that can be reached
     * via the rover with assigned ip address i1 and updates their metric to 16.
     */
    {

        for(InetAddress key:route_table.keySet()){
            RoverEntry temp_roverEntry = route_table.get(key);
            if (temp_roverEntry.getVia_router().equals(i1)){
                temp_roverEntry.change_metric_and_via(16,i1);
            }
        }

    }





    public void deposit_packet(InetAddress sender_ip, byte[] msg)
    /**
     * Allows the external client class to deposit an incoming packet
     * onto the blocking queue which acts as a buffer for incoming packets.
     * In order to identify which ip address sent the packet we add
     * the ip address and packet to a Pair class that acts as a tuple
     * this tuple is added to the blocking queue
     */
    {
        Pair<InetAddress, byte[]> p1 = new Pair<>(sender_ip, msg);
        try {
            incoming_packets.put(p1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] return_rip_template(byte[] packet, int command)
    /**
     * returns an rip template by filling up the first 2 bytes
     * which are command and version.
     */
    {
        //command = 1 (request), command = 2 for response
        if (command == 1) {
            packet[0] = 1;
            packet[1] = 2;//for version = ripv2
        } else if (command == 2) {
            packet[0] = 2;
            packet[1] = 2;
        }
        return packet;
    }

    public byte[] add_entry_to_packet(int startPos, byte[] packet, int afi,
                                      InetAddress destination,
                                      InetAddress nextHop, int metric)
    /**
     * Adds an individual entry to the ripv2 packet based on start position
     * and other information given.
     */
    {
        if (afi == 2) //this is for all messages except a request for the whole
        //routing table.
        {
            //AFI is byte 4 to 5, here it is 2 for ip
            packet[startPos] = 0;
            packet[startPos + 1] = 2;
            byte[] dest = destination.getAddress();
            byte[] via = nextHop.getAddress();
            byte[] subnet = {(byte)255,(byte)255,(byte)255,(byte)0};
            //adding ip address to byte 4 to 7
            for (int i = 0; i < 4; i++) {

                packet[startPos + 4 + i] = dest[i];
            }
            for (int i = 0; i < 4; i++) {
                packet[startPos + 8 + i] = subnet[i];
            }
            //
            for (int i = 0; i < 4; i++) {
                packet[startPos + 12 + i] = via[i];
            }
            //subnet mask

            //the last 4 bytes represent the metric, as metric can not go
            //beyond 16 (which is 0b10000) we only need to update the last
            //of the 4 bytes.
            packet[startPos + 19] = (byte) metric;
        } else if (afi == 0) //single entry in packet mainly
            // used to request entire routing table
        {
            packet[startPos] = 0;
            packet[startPos + 1] = 0;
            packet[startPos + 19] = 16;
//            System.out.println("Adding 16 at 142");

        }
        return packet;
    }

    //request - request for whole table
    //request - request for specific entry
    //response - response to request (whole table)
    //response - response to request (specific table)
    //response - regular updates
    //response - triggered updates

    public byte[] prepare_entire_ripPacket(int command, byte[] rip_packet)
    /**
     * This provides the rip packet that has to be sent to the receiver
     * via the sender class.
     */
    {
        rip_packet = return_rip_template(rip_packet, command);
        //as there are 10 ip addresses and the ripv2 header takes up 4 bytes
        //with each entry taking up 20 bytes, we start with 4 and end at 204
        //to have 10 entries.
        int start_entry = 4;
        InetAddress temp_address;
        RoverEntry temp_roverEntry;

        for(InetAddress key : route_table.keySet()){
            temp_address = key;
            temp_roverEntry = route_table.get(key);
//            System.out.println("key = "+key+", re_via "+temp_roverEntry.getVia_router()+", r-r-ip = "+receiver_real_ip+", metric = "+temp_roverEntry.getMetric());
//            if(temp_roverEntry.getVia_router().equals(receiver_real_ip)){
//                //split-horizon, any entry learned through a router
//                //is to be sent as a metric of 16 and via = current router.
//                try {
////                    System.out.println("line 176: via router = "+temp_roverEntry.getVia_router()+", " +
////                            "real receiver ip"+receiver_real_ip+", temp address = "+temp_address);
//                    rip_packet = add_entry_to_packet(start_entry, rip_packet,
//                            2, temp_address, this_real_ip,
//                            20);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//
//            } else
//            if(temp_roverEntry.getVia_router().equals(receiver_real_ip)){
//                //skip RTE that contains receivers entry.
//                continue;
//
//            }
//            else {
//                System.out.println("adding "+temp_address+","+ temp_roverEntry.getVia_router()+","+
//                        temp_roverEntry.getMetric()+" to packet");
                rip_packet = add_entry_to_packet(start_entry, rip_packet,
                        2, temp_address, temp_roverEntry.getVia_router(),
                        temp_roverEntry.getMetric());
            start_entry+=20;
            }


//        }
        return rip_packet;
    }



    //request - request for whole table
    //request - request for specific entry
    //response - response to request (whole table)
    //response - response to request (specific table)
    //response - regular updates
    //response - triggered updates

    public byte [] process_incoming_request(InetAddress to_address,InetAddress to_real_ip, byte[] packet)
    /**
     * This method helps process requests packets (where the command byte is 1).
     */
    {
        //if there is one request only, address family identifier is 0
        //and metric is 16. We send the entire routing table.
        byte[] response_packet = new byte[504];
        response_packet[0]= (byte)2;
        byte[] address_family_identifier = new byte[2];
        System.arraycopy(packet, 5, address_family_identifier,
                0, 2);
        byte[] dest_metric = new byte[4];
        System.arraycopy(packet, 20, dest_metric, 0, 4);
        if ((address_family_identifier[1] & 0xff) == 0 &&
                (address_family_identifier[0] & 0xff) == 0
                && (dest_metric[3] & 0xff) == 16) {
            //send the entire table
//             response_packet = prepare_entire_ripPacket(to_address,to_real_ip,2,response_packet);
        } else if ((address_family_identifier[1] & 0xff) == 0 &&
                (address_family_identifier[0] & 0xff) == 2) {
            //send only requested RTEs.
            //not impletementing this currently.

        }
        return response_packet;
    }

    public void print_packet(byte [] packet, InetAddress rcvr){
        try {
            byte[] temp = new byte[4];
            InetAddress dest;
            InetAddress next_hop;
            int metric = 0;
            for (int i = 4; i < 504; i = i + 20) {
                System.arraycopy(packet, i+4, temp, 0, 4);
                dest = InetAddress.getByAddress(temp);
                System.arraycopy(packet, i+12, temp, 0, 4);
                next_hop = InetAddress.getByAddress(temp);
                if(dest.equals(InetAddress.getByName("0.0.0.0"))){
                    break;
                }
                System.out.println("create packet for = "+rcvr+"," +
                        " Destination address = "+dest+", Next hop = "+next_hop+
                        ", metric = "+(packet[i+19]&0xff));

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }



    public void process_incoming_response(InetAddress from_address,
                                          byte[] packet)
    /**
     * This method updates the routing table based on the entries in the
     * incoming response packet.
     */
    {
        //fixed_cost is the cost it takes to go from this RoverEntry to the
        //rover that has sent the packet. This is 1 as an incoming packet
        //directly from the rover tells us that is is directly connected to us.

        int fixed_cost = 1;
        byte[] dest_byte = new byte[4];
        byte[] next_hop = new byte[4];
        byte[] metric = new byte[1];
        InetAddress dest_address;
        InetAddress via;
        int temp_metric;
        for(int i=8;i<504;i=i+20){
            System.arraycopy(packet,i,dest_byte,0,4);
            System.arraycopy(packet,i+8,next_hop,0,4);
            System.arraycopy(packet,i+15,metric,0,1);
            //update table
            try {
                dest_address = InetAddress.getByAddress(dest_byte);
                via = InetAddress.getByAddress(next_hop);
//                System.out.println("rcvd from "+from_address+",destAdd = "+dest_address+", via = "+via+", metric = "+(metric[0]&0xff));
                if(dest_address.equals(InetAddress.getByName("0.0.0.0")) &&
                        (metric[0]&0xff)==0){
                    continue;
                    //to prevent iterating through empty entries.
                }
                /**
                 * if there is no such rover with the address mentioned in the
                 * packet, create rover set it to inactive as only rovers that
                 * directly contact this rover is active.
                 */
                if(!route_table.containsKey(dest_address)){
                    if(dest_address.equals(InetAddress.getByName("0.0.0.0"))){
                        continue;
                    }
//                    System.out.println("RT.java line 269 :Creating router entry at routing_table 269");
                    int cost = (metric[0]&0xff)+fixed_cost;
                    if(cost>16){
                        cost=16;
                    }
                    RoverEntry temp_roverEntry = new RoverEntry(InetAddress.getByName("0.0.0.0"),
                            dest_address,via,this_assigned_ip,
                            cost,
                            this,false);
                    temp_roverEntry.start();
                    route_table.put(dest_address, temp_roverEntry);

                }else {
                    RoverEntry temp_roverEntry = route_table.get(dest_address);
                    temp_metric = temp_roverEntry.getMetric();
                    //temp metric is the cost it takes to go to temp rover whose
                    //ip address is in dest_address.
                    //there are three possibilities:
                    if ((fixed_cost + (metric[0] & 0xff)) > temp_metric) {
                        //if the next hop in the rover object is the same as the
                        //ip address of the packer sender then we update
                        if (temp_roverEntry.getVia_router().equals(from_address)) {
//                        System.out.println("RT.java line 292 :updating routing table");
//                        System.out.println("RT.java line 293 : packet from "+from_address);
                            int cost = (fixed_cost + (metric[0] & 0xff));
                            if (cost > 16) {
                                cost = 16;
                            }
//                            System.out.println("changing at RT_318 for " + temp_roverEntry.getAssigned_address() + ", cost = " + cost + ", from address = " + from_address);
                            temp_roverEntry.change_metric_and_via(cost, from_address);
                        }
                        //we do not update if the via router address is different

                    } else if ((fixed_cost + (metric[0] & 0xff)) < temp_metric) {
//                    System.out.println("RT.java line 299 :updating routing table");
//                    System.out.println("RT.java line 300 : packet from "+from_address);
//                        System.out.println("changing at RT_326 for " + temp_roverEntry.getAssigned_address() + ", cost = " + (fixed_cost + (metric[0] & 0xff)) + ", from address = " + from_address);
                        temp_roverEntry.change_metric_and_via(
                                (fixed_cost + (metric[0] & 0xff)), from_address);

                    }
                }
                // we do not update if the metric is the same.
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    public List<InetAddress> obtain_neighbours(){
        List<InetAddress> neighbour_list = new ArrayList<>();
        for(InetAddress key : route_table.keySet()){

            RoverEntry temp_roverEntry = route_table.get(key);
            //a neighbour is someone who is one hop away
//            if(temp_roverEntry.getMetric()==1){
                neighbour_list.add(key);
//            }

        }
        return neighbour_list;
    }

    public byte[] multicast_hello(){
        byte[] hello = new byte[504];
        hello = return_rip_template(hello,1);
        hello = add_entry_to_packet(4,hello,2,
                this_assigned_ip,this_real_ip,0);
        //multicast hello has the route tag value as 1. This ensures that
        //rovers directly connected to the current rover, will discard this
        //after checking their entry table. If a firewall has been lifted,
        //multicast hello will allow a new rover to add this rover as immediate
        //neighbour.
        hello[7]=1;
        return hello;
    }

    public void send_regular_update(){
        /**
         * extract list of neighbours, send them updates according to split horizon
         */
        try {
            byte[] packet = new byte[504];
//        List<InetAddress> neighbours = obtain_neighbours();
            for (InetAddress i1 : route_table.keySet()) {

                if (i1.equals(this_assigned_ip)) {
//                    System.out.println("skipping for " + i1);
                    continue;
                }
                RoverEntry temp_roverEntry = route_table.get(i1);
//                System.out.println("creating packet for " + i1);
                packet = prepare_entire_ripPacket(2, packet);

                InetAddress sender_address = temp_roverEntry.getReal_router_address();
//            print_packet(packet,sender_address);
//            System.out.println("RT342:send_regular_update");
                Thread.sleep(ThreadLocalRandom.current().nextLong(600, 800));
                new Sender(packet, sender_address, sender_socket, "RT342:send_regular_update to " + i1).start();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        //hello message via multicast, use route tag. update process_incoming_packet to handle route tags.
        //hello message has to be request for entire table and route tag set to 1
        //obtain list of neighbours
        //use prepare entire rip packet
        //send
        //this has to be implemented as thread
    }

    public byte[] prepare_triggered_update(InetAddress router_address,
                                         InetAddress via_address,int metric){
        byte [] packet = new byte[504];
        packet = return_rip_template(packet,2);
        packet = add_entry_to_packet(4,packet,2,router_address,via_address,metric);

        return packet;
    }


    public void process_incoming_packet(Pair<InetAddress, byte[]> p1) {
        try {
            //(0-7 is command byte) check if command is request or response
            byte[] packet = p1.getB();
            InetAddress receiver_real_ip = p1.getA();
            //to discard multicast updates from the same rover.
            byte[] dest_ip = new byte[4];
            System.arraycopy(packet, 8, dest_ip, 0, 4);
            if(dest_ip[0]==0){
                return;
            }
            process_incoming_response(receiver_real_ip, packet);
            //checking if route tag==1 to see if message is multicast hello.
//            if(receiver_real_ip.equals(this_real_ip)||receiver_dest_ip.equals(this_assigned_ip)){
//                return;
//            }
//            if(packet[7]==1 && (route_table.containsKey(receiver_dest_ip))){
//                RoverEntry temp_rover_entry = route_table.get(receiver_dest_ip);
//                System.out.println("changing at RT_419 for "+temp_rover_entry.getAssigned_address()+", cost = 1, from address = "+this_real_ip);
//                temp_rover_entry.change_metric_and_via(1,this_real_ip);
////                temp_rover_entry.update_timer();
//
//            }else
//            if(packet[7]==1 && !(route_table.containsKey(receiver_dest_ip))){
////                System.out.println("RT.java line 380: Receiver assignedip "+receiver_dest_ip);
////                System.out.println("RT.java line 381: Receiver real ip "+receiver_real_ip);
////                System.out.println("RT.java line 382: Creating router entry at routing_table 380");
//                RoverEntry temp_roverEntry =new RoverEntry(receiver_real_ip,
//                        receiver_dest_ip,this_assigned_ip,this_assigned_ip,
//                        1,this,true);
//                route_table.put(receiver_dest_ip, temp_roverEntry);
//                temp_roverEntry.start();
//                return;
//            }

//            if (!route_table.containsKey(receiver_dest_ip)) {
//                //this id for rovers that do not exist in the Route table
//                //any rover that can send a message is directly connected to
//                //this rover and hence has a metric of 1.
////                System.out.println("RT.java 395: creating new rover entry");
////                System.out.println("RT.java 396: Receiver assignedip "+receiver_dest_ip);
////                System.out.println("RT.java 397: Receiver real ip "+receiver_real_ip);
//                RoverEntry temp_roverEntry =new RoverEntry(receiver_real_ip,
//                        receiver_dest_ip,this_assigned_ip,this_assigned_ip,
//                        1,this,true);
//                route_table.put(receiver_dest_ip, temp_roverEntry);
//                temp_roverEntry.start();
//
//
//            } else if(route_table.containsKey(receiver_dest_ip))
//            //this elseif condition is for Route table entries that exist as inactive rovers
//            //i.e rovers that are not connected directly to this rover but
//            //whose entry has been learned from another rover. As they have now
//            //contacted us directly, they now become an active rover and their
//            //metric has to be changed to 1.
//            {
//                /*
//                checkchangemetric
//                 */
//                RoverEntry temp_roverEntry = route_table.get(receiver_dest_ip);
//                if(temp_roverEntry.getMetric()!=1){
//                    //update metric to 1, set next hop to current rover
//                    System.out.println("changing at RT_462 for "+temp_roverEntry.getAssigned_address()+", cost = 1, from address = "+this_real_ip);
//                    temp_roverEntry.change_metric_and_via(1,this_real_ip);
//                    //update real ip address of rover
//                    temp_roverEntry.setReal_router_address(receiver_real_ip);
//                }
//            }
//            RoverEntry temp_roverEntry = route_table.get(receiver_dest_ip);
//            //as soon as we get a packet from a rover, we update rover's last_updated
//            //metric
////            temp_roverEntry.update_timer();
//            temp_roverEntry.activate_rover();
//            byte[] command_byte = new byte[1];
//            System.arraycopy(packet, 0, command_byte, 0, 1);
//            //if the command byte is 1 then it is a request.
//            //the request is processed
////            if ((command_byte[0] & 0xff) == 1) {
////                //prepare the packet to send according to request
////                byte[] rip_packet = process_incoming_request(receiver_dest_ip,receiver_real_ip, packet);
////                //spawn a thread to send a packet.
//////                System.out.println("RT.java 446: Sending from process_incoming packet after process incoming request");
//////                System.out.println("RT.java 447: Process_incoming response - receiver_dest_ip = "+receiver_dest_ip);
//////                System.out.println("RT.java 448: Process_incoming response - receiver_real_ip = "+receiver_real_ip);
////                new Sender(rip_packet, receiver_real_ip,sender_socket,"RT467 incoming request "+receiver_real_ip+" cmd= "+(packet[0]&0xff)).start();
////            } else if ((command_byte[0] & 0xff) == 2) {
//                //use the information from the incoming packet to update table.
////                System.out.println("RT.java 452: Process_incoming response");
////                System.out.println("RT.java 453: Process_incoming response - receiver_dest_ip = "+receiver_dest_ip);
////                System.out.println("RT.java 454: Process_incoming response - receiver_real_ip = "+receiver_real_ip);

//            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void print_routing_table(){
        try {
            System.out.print("Address\t");
            System.out.print("Next Hop\t");
            System.out.println("Cost");
            System.out.println("===================================================================");
            for (InetAddress i1 : route_table.keySet()) {
                RoverEntry temp_roverEntry = route_table.get(i1);
                InetAddress dest = temp_roverEntry.getAssigned_address();
                System.out.print(dest+"/24" + "\t");//address
                String via = temp_roverEntry.getVia_router().getHostAddress();
                System.out.print(via + "\t");
                int cost = temp_roverEntry.getMetric();
                if(cost>16){
                    cost=16;
                }
                System.out.println(cost);

            }
        }catch (Exception e){
            e.printStackTrace();
            /**
             * remove this
             */
        }
    }

    public void run() {
        try {

            while(active_table){

                Pair<InetAddress, byte[]> p1 = incoming_packets.take();
                process_incoming_packet(p1);


            }
            //extract from blocking queue
            //process update
            //sleep for 5 seconds
            //send regular updates
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
