package net.sistr.littlemaidrebirth.entity.iff;

public enum IFFTag {
    UNKNOWN(0, "Unknown"),
    FRIEND(1, "Friendly"),
    ENEMY(2, "Enemy");

    private final int id;
    private final String name;

    IFFTag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static IFFTag getTagFromId(int id) {
        for (IFFTag tag : IFFTag.values()) {
            if (tag.getId() == id) {
                return tag;
            }
        }
        throw new IllegalArgumentException("そのIDのIFFTagは存在しません。 : " + id);
    }

    public static IFFTag getTagFromName(String name) {
        for (IFFTag tag : IFFTag.values()) {
            if (tag.getName().equals(name)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("その名前のIFFTagは存在しません。 : " + name);
    }
}
