import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

class Client{

    public static void main( String[] args){
        if (args.length < 1) {
            System.err.print( "Usage: port\n" );
            System.exit( 1 );
        }

        int port = Integer.parseInt( args[0] );

        try {
            Socket s = new Socket(InetAddress.getByName( "localhost" ), port);
            System.out.print( "Connected server on port " + port + "\n" );

            ClientActions handler = new ClientActions(s);

            handler.run();
        } catch (Exception e) {
            System.err.print( "Cannot open socket: " + e );
            System.exit( 1 );
        }


    }
}
