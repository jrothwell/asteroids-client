import alexh.weak.Dynamic;

/**
 * Created by joro on 11/09/2016.
 */
public class Ship {
    public String name;
    public Double bearing;
    public Double distance;

    public Ship(Dynamic d) {
        this.name = d.get(0).asString();
        this.bearing = d.get(1).as(Double.class);
        this.distance = d.get(2).as(Double.class);
    }

    public Double getBearing() {
        return bearing;
    }

    public String getName() {
        return name;
    }

    public Double getDistance() {
        return distance;
    }

}
