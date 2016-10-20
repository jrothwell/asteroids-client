import alexh.weak.Dynamic;

/**
 * Created by joro on 11/09/2016.
 */
public class Asteroid implements Target {
    public Integer id;
    public Double bearing;
    public Double size;
    public Double distance;

    public Asteroid(Dynamic d) {
        this.id = d.get(0).as(Integer.class);
        this.bearing = d.get(1).as(Double.class);
        this.size = d.get(2).as(Double.class);
        this.distance = d.get(3).as(Double.class);
    }

    public Double getTheta() {
        return bearing;
    }

    public Integer getId() {
        return id;
    }

    public Double getSize() {
        return size;
    }

    public Double getDistance() {
        return distance;
    }
}
