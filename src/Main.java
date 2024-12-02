import ServerSide.Server;

public class Main {
    public static void main(String[] args) throws Exception{
        // Start server
        Server server = new Server(23465);
        server.start();
    }
}