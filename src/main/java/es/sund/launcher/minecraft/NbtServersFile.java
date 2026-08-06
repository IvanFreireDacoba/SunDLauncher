package es.sund.launcher.minecraft;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector/escritor NBT mínimo, suficiente para leer y escribir un servers.dat
 * real de Minecraft (a diferencia de level.dat, servers.dat NO va comprimido
 * con gzip). No es un NBT de propósito general: soporta los tipos de tag que
 * pueden aparecer en un servers.dat (Compound, List, String, Byte, y el resto
 * de tipos numéricos por si acaso), preservando cualquier tag desconocido tal
 * cual para no perder datos si Minecraft añade campos nuevos.
 *
 * DataInputStream/DataOutputStream ya son big-endian, y su readUTF/writeUTF
 * ya usan el mismo formato que TAG_String en NBT (2 bytes de longitud + UTF-8
 * modificado), así que se reutilizan directamente en vez de reimplementar la
 * codificación de texto a mano.
 */
final class NbtServersFile {

    static final byte TAG_END = 0;
    static final byte TAG_BYTE = 1;
    static final byte TAG_SHORT = 2;
    static final byte TAG_INT = 3;
    static final byte TAG_LONG = 4;
    static final byte TAG_FLOAT = 5;
    static final byte TAG_DOUBLE = 6;
    static final byte TAG_BYTE_ARRAY = 7;
    static final byte TAG_STRING = 8;
    static final byte TAG_LIST = 9;
    static final byte TAG_COMPOUND = 10;
    static final byte TAG_INT_ARRAY = 11;
    static final byte TAG_LONG_ARRAY = 12;

    /** Un tag con nombre dentro de un compound: type es uno de los TAG_* de arriba. */
    record Tag(byte type, Object value) {}

    /** Payload de un TAG_List: todos los elementos comparten elementType, y no llevan nombre propio. */
    record NbtList(byte elementType, List<Object> elements) {}

    static Map<String, Tag> readCompoundFile(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            byte rootType = in.readByte();
            if (rootType != TAG_COMPOUND) {
                throw new IOException("Se esperaba un TAG_Compound raíz, encontrado tipo " + rootType);
            }
            in.readUTF(); // nombre del tag raíz (normalmente vacío), se descarta
            return readCompoundBody(in);
        }
    }

    static void writeCompoundFile(Path path, Map<String, Tag> root) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");
            writeCompoundBody(out, root);
        }
    }

    private static Map<String, Tag> readCompoundBody(DataInputStream in) throws IOException {
        Map<String, Tag> map = new LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == TAG_END) {
                return map;
            }
            String name = in.readUTF();
            map.put(name, new Tag(type, readPayload(in, type)));
        }
    }

    private static void writeCompoundBody(DataOutputStream out, Map<String, Tag> map) throws IOException {
        for (Map.Entry<String, Tag> entry : map.entrySet()) {
            Tag tag = entry.getValue();
            out.writeByte(tag.type());
            out.writeUTF(entry.getKey());
            writePayload(out, tag.type(), tag.value());
        }
        out.writeByte(TAG_END);
    }

    private static Object readPayload(DataInputStream in, byte type) throws IOException {
        switch (type) {
            case TAG_BYTE:
                return in.readByte();
            case TAG_SHORT:
                return in.readShort();
            case TAG_INT:
                return in.readInt();
            case TAG_LONG:
                return in.readLong();
            case TAG_FLOAT:
                return in.readFloat();
            case TAG_DOUBLE:
                return in.readDouble();
            case TAG_BYTE_ARRAY: {
                int len = in.readInt();
                byte[] bytes = new byte[len];
                in.readFully(bytes);
                return bytes;
            }
            case TAG_STRING:
                return in.readUTF();
            case TAG_LIST: {
                byte elementType = in.readByte();
                int len = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(len, 0));
                for (int i = 0; i < len; i++) {
                    list.add(readPayload(in, elementType));
                }
                return new NbtList(elementType, list);
            }
            case TAG_COMPOUND:
                return readCompoundBody(in);
            case TAG_INT_ARRAY: {
                int len = in.readInt();
                int[] ints = new int[len];
                for (int i = 0; i < len; i++) ints[i] = in.readInt();
                return ints;
            }
            case TAG_LONG_ARRAY: {
                int len = in.readInt();
                long[] longs = new long[len];
                for (int i = 0; i < len; i++) longs[i] = in.readLong();
                return longs;
            }
            default:
                throw new IOException("Tipo de tag NBT no soportado: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream out, byte type, Object value) throws IOException {
        switch (type) {
            case TAG_BYTE -> out.writeByte((Byte) value);
            case TAG_SHORT -> out.writeShort((Short) value);
            case TAG_INT -> out.writeInt((Integer) value);
            case TAG_LONG -> out.writeLong((Long) value);
            case TAG_FLOAT -> out.writeFloat((Float) value);
            case TAG_DOUBLE -> out.writeDouble((Double) value);
            case TAG_BYTE_ARRAY -> {
                byte[] bytes = (byte[]) value;
                out.writeInt(bytes.length);
                out.write(bytes);
            }
            case TAG_STRING -> out.writeUTF((String) value);
            case TAG_LIST -> {
                NbtList nbtList = (NbtList) value;
                out.writeByte(nbtList.elementType());
                out.writeInt(nbtList.elements().size());
                for (Object element : nbtList.elements()) {
                    writePayload(out, nbtList.elementType(), element);
                }
            }
            case TAG_COMPOUND -> writeCompoundBody(out, (Map<String, Tag>) value);
            case TAG_INT_ARRAY -> {
                int[] ints = (int[]) value;
                out.writeInt(ints.length);
                for (int i : ints) out.writeInt(i);
            }
            case TAG_LONG_ARRAY -> {
                long[] longs = (long[]) value;
                out.writeInt(longs.length);
                for (long l : longs) out.writeLong(l);
            }
            default -> throw new IOException("Tipo de tag NBT no soportado: " + type);
        }
    }

    private NbtServersFile() {}
}
