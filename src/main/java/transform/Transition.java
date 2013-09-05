package transform;

// Simple pojo to help sorting and de-duping of transitions
public class Transition implements Comparable<Transition> {
    private final String name;
    private final String source;
    private final String target;
    private final String key;

    public Transition(String name, String source, String target) {
        this.name = name;
        this.source = source;
        this.target = target;
        key = name + "/" + source + "/" + target;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public int compareTo(Transition o) {
        return key.compareTo(o.key);
    }

    public String getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof Transition) || o == null ) {
            return false;
        }
        return key.equals(((Transition) o).key);
    }

    public String getSource() {
        return source;
    }

    public String getName() {
        return name;
    }
}
