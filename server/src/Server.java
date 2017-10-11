import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

class Server {

    static public void
    waitForClients ( ServerSocket s ) {
        ServerControl registry = new ServerControl();

        try {
            while (true) {
                Socket c = s.accept();
                ServerActions handler = new ServerActions( c, registry );
                new Thread( handler ).start ();
            }
        } catch ( Exception e ) {
            System.err.print( "Cannot use socket: " + e );
        }

    }

    public static void main ( String[] args ) {
        if (args.length < 1) {
            System.err.print( "Usage: port\n" );
            System.exit( 1 );
        }

        int port = Integer.parseInt( args[0] );

        try {
            ServerSocket s = new ServerSocket( port, 5, InetAddress.getByName( "localhost" ) );
            System.out.print( "Started server on port " + port + "\n" );
            waitForClients( s );
        } catch (Exception e) {
            System.err.print( "Cannot open socket: " + e );
            System.exit( 1 );
        }

    }

}
