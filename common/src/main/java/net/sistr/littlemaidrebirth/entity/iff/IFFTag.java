package net.sistr.littlemaidrebirth.entity.iff;

/**
 * どのように敵対するかを示すクラス
 * */
public enum IFFTag {
    UNKNOWN(0, "Unknown", true, false),
    FRIEND(1, "Friendly", false, false),
    ENEMY(2, "Enemy", true, true);

    private final int id;
    private final String name;
    private final boolean active;
    private final boolean passive;

    IFFTag(int id, String name, boolean active, boolean passive) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.passive = passive;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * IFFTagの数値idからIFFTagを得る
     */
    public static IFFTag getTagFromId(int id) {
        for (IFFTag tag : IFFTag.values()) {
            if (tag.getId() == id) {
                return tag;
            }
        }
        throw new IllegalArgumentException("そのIDのIFFTagは存在しません。 : " + id);
    }

    /**
     * IFFTagの名前からIFFTagを得る
     */
    public static IFFTag getTagFromName(String name) {
        for (IFFTag tag : IFFTag.values()) {
            if (tag.getName().equals(name)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("その名前のIFFTagは存在しません。 : " + name);
    }

    /**
     * 反撃可能であるかを返す
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 常時攻撃対象であるかを返す
     */
    public boolean isPassive() {
        return passive;
    }
}
