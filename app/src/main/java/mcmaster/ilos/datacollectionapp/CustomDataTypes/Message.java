package mcmaster.ilos.datacollectionapp.CustomDataTypes;

/* Represents the text displayed on the CardView */
public class Message {

    private String header;
    private String body;

    public Message(String header, String body) {
        this.header = header;
        this.body = body;
    }

    public String getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}

