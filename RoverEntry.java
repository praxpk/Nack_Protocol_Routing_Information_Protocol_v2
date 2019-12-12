import java.net.InetAddress;


public class RoverEntry extends Thread{
    private InetAddress real_router_address;
    private InetAddress via_router;
    private int metric;
    private long last_updated;
    private boolean route_change_flag;
    private boolean active_rover;
    private boolean terminate_rover;
    private Routing_table rt_object;
    private InetAddress assigned_address;
    private InetAddress assigned_address_current_rover;

    public RoverEntry(InetAddress real_router_address, InetAddress assigned_address,
                      InetAddress via_router, InetAddress assigned_address_current_rover,
                      int metric, Routing_table rt_object,
                      boolean active_rover){
        /**
         * set rover as inactive if its not a direct neighbour
         * activate only if direct message received
         */

        this.last_updated = System.currentTimeMillis();
        this.real_router_address = real_router_address;
        this.via_router = via_router;
        this.metric = metric;
        this.route_change_flag = false;
        this.active_rover = active_rover;
        this.terminate_rover = false;
        this.rt_object = rt_object;
        this.assigned_address = assigned_address;
        this.assigned_address_current_rover = assigned_address_current_rover;
//        System.out.println("Rover entry.java line 36:assigned address = "+assigned_address+" initialized." +
//                " Real Router Address = "+real_router_address+". Via_router = "+via_router);
    }


    public InetAddress getReal_router_address() {
        return real_router_address;
    }

    public InetAddress getAssigned_address() {
        return assigned_address;
    }


    public void update_timer(){
        this.last_updated = System.currentTimeMillis();
    }

    public InetAddress getVia_router() {
        return via_router;
    }


    public void setReal_router_address(InetAddress real_router_address){
        this.real_router_address = real_router_address;
    }

    public void setMetric(int m){
        this.metric = m;
        this.update_timer();
    }
    public int getMetric() {
        return metric;
    }



    public void change_metric_and_via(int metric, InetAddress via){
        this.metric = metric;
        this.via_router = via;
        this.last_updated = System.currentTimeMillis();
    }

    public void terminate_rover(){
        this.terminate_rover = true;
    }


    public void run(){
        while (!terminate_rover) {
            try{
                    if ((System.currentTimeMillis() - last_updated) > 20000) {
                        //update metric here
                        metric = 16;
                        //create single entry packet
                        //obtain neighbours
                        //send triggered update
                        byte[] packet = rt_object.prepare_triggered_update
                                (assigned_address, assigned_address_current_rover, metric);
                        //update the metric of all rovers that go through this rover as 16
                        rt_object.update_to_16(assigned_address);
//                        if(((System.currentTimeMillis() - last_updated) > 30000)) {
//                            for (InetAddress i1 : rt_object.obtain_neighbours()) {
//                                InetAddress real_ip_of_receiver = rt_object.obtain_real_ip_address(i1);
////
//                                new Sender(packet, real_ip_of_receiver, rt_object.sender_socket, "RE137: time<10000 ").start();
//                            }
//                        }


                    }

        }catch(Exception e){
            e.printStackTrace();

        }
        }
    }
}
