import com.google.gson.*;

class UserDescription implements Comparable {

    int id;		             // id extracted from the CREATE command
    JsonElement description;     // JSON user's description
    String uuid;		     // User unique identifier (across sessions)

    UserDescription ( int id, JsonElement description ) {
        this.id = id;
        this.description = description;
        uuid = description.getAsJsonObject().get( "uuid" ).getAsString();
        description.getAsJsonObject().addProperty( "id", new Integer( id ) );
    }

    UserDescription ( int id ) {
        this.id = id;
    }

    public int
    compareTo ( Object x ) {
        return ((UserDescription) x).id - id;
    }

}
