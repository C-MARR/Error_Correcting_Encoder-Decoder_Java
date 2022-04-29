package correcter;

import java.io.*;
import java.util.*;

public class Main {
    static final int[] PARITY_SEQUENCE = new int[] {0, 1, 3, 7};

    public static void main(String[] args) {
        File send = new File("send.txt");
        File encoded = new File("encoded.txt");
        File received = new File("received.txt");
        File decoded = new File("decoded.txt");
        menu(send, encoded, received, decoded);
    }

    protected static void menu(File send, File encoded, File received, File decoded) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String input = scanner.nextLine();
        switch (input) {
            case "encode":
                encode(send, encoded);
                break;
            case "send":
                send(encoded, received);
                break;
            case "decode":
                decode(received, decoded);
                break;
        }
        System.out.println();
    }


    protected static void encode(File send, File encoded) {
        ArrayList<String> shifter = convertToBinary(send);
        try (FileOutputStream encoder = new FileOutputStream(encoded)) {
            StringBuilder text = new StringBuilder();
            String generatedBin = String.join("", shifter);
            String hex = generateHexString(generatedBin);
            String binary = generateBinaryString(generatedBin);
            StringBuilder doubleByte = new StringBuilder();
            for (String s : shifter) {
                text.append((char) Integer.parseInt(s, 2));
            }
            for (String byt : shifter) {
                StringBuilder addParity = new StringBuilder(".." + byt)
                        .insert(3, ".")
                        .insert(7, "0..")
                        .insert(11, ".")
                        .append("0");
                int count = 0;
                for (int i = 0; i < addParity.length(); i++) {
                    if (addParity.charAt(i) != '.') continue;
                    if (i % Byte.SIZE == PARITY_SEQUENCE[0]) {
                        for (int j = i % Byte.SIZE; j < Byte.SIZE; j += 2) {
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                        }
                        addParity.replace(i, i + 1, count % 2 == 1 ? "1" : "0") ;
                        count = 0;
                    } else if (i % Byte.SIZE == PARITY_SEQUENCE[1]) {
                        for (int j = i % Byte.SIZE; j < Byte.SIZE; j += 3) {
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                            j++;
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                        }
                        addParity.replace(i, i + 1, count % 2 == 1 ? "1" : "0") ;
                        count = 0;
                    } else if (i % Byte.SIZE == PARITY_SEQUENCE[2]) {
                        for (int j = i % Byte.SIZE; j < Byte.SIZE; j += 4) {
                            j++;
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                            j++;
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                            j++;
                            if (addParity.charAt(i < Byte.SIZE ? j : j + Byte.SIZE) == '1') count++;
                        }
                        addParity.replace(i, i + 1, count % 2 == 1 ? "1" : "0") ;
                        count = 0;
                    }
                }
                doubleByte.append(addParity);
            }

            for (int i = 0; i < doubleByte.length(); i += Byte.SIZE) {
                String newByte = doubleByte.substring(i, i + Byte.SIZE);
                encoder.write(Integer.parseInt(newByte, 2));
            }

            System.out.printf("\n%s:" +
                    "\ntext view: %s" +
                    "\nhex view: %s" +
                    "\nbin view: %s" +
                    "\n", send.getName(), text, hex, binary);

            String parityHex = generateHexString(doubleByte.toString());
            String parity = generateBinaryString(doubleByte.toString());
            String expand = generateBinaryString(doubleByte.toString(), true);
            System.out.printf("\n%s:" +
                    "\nexpand: %s" +
                    "\nparity: %s" +
                    "\nhex view: %s", encoded.getName(), expand, parity, parityHex);
        }
        catch (IOException e) {
            System.out.println("Write Error");
        }

    }

    protected static void send(File encoded, File received) {
        ArrayList<String> encode = convertToBinary(encoded);
        StringBuilder scramble = new StringBuilder();
        Random random = new Random();
        for (String s : encode) {
            StringBuilder corrupt = new StringBuilder(s);
            int randomNumber = random.nextInt(7);
            corrupt.setCharAt(randomNumber + 1, corrupt.charAt(randomNumber + 1) == '1' ? '0' : '1');
            scramble.append(corrupt);
        }
        try (FileOutputStream receive = new FileOutputStream(received)) {
            String encodedHex = generateHexString(String.join("", encode));
            String encodedBinary = generateBinaryString(String.join("", encode));
            String receivedHex = generateHexString(String.join("", scramble.toString()));
            String receivedBinary = generateBinaryString(String.join("", scramble.toString()));

            System.out.printf("\n%s:" +
                    "\nhex view: %s" +
                    "\nbin view: %s" +
                    "\n" +
                    "\n%s:" +
                    "\nhex view: %s" +
                    "\nbin view: %s" +
                    "\n", encoded.getName(), encodedHex, encodedBinary,
                    received.getName(), receivedHex, receivedBinary);

            for (int i = 0; i < scramble.length(); i += Byte.SIZE) {
                String newByte = scramble.substring(i, i + Byte.SIZE);
                receive.write(Integer.parseInt(newByte, 2));
            }
        } catch (IOException e) {
            System.out.println("Write Error");
        }
    }

    protected static void decode(File received, File decoded) {
        ArrayList<String> receive = convertToBinary(received);
        StringBuilder fixer = new StringBuilder();

        for (String byt : receive) {
            StringBuilder badByte = new StringBuilder(byt);
            int badBitIndex = 0;
            for (int i = 0; i <= PARITY_SEQUENCE[2]; i++) {
                int previousParity = Integer.parseInt(byt.substring(i, i + 1));
                if (Integer.parseInt(byt.substring(7, 8)) == 1) {
                    badBitIndex = 8;
                    break;
                }
                if (i == PARITY_SEQUENCE[0]) {
                    int count = 0;
                    if (Integer.parseInt(byt.substring(2, 3)) == 1) count++;
                    if (Integer.parseInt(byt.substring(4, 5)) == 1) count++;
                    if (Integer.parseInt(byt.substring(6, 7)) == 1) count++;
                    if (count % 2 != previousParity) {
                        badBitIndex += i + 1;
                    }
                } else if (i == PARITY_SEQUENCE[1]) {
                    int count = 0;
                    if (Integer.parseInt(byt.substring(2, 3)) == 1) count++;
                    if (Integer.parseInt(byt.substring(5, 6)) == 1) count++;
                    if (Integer.parseInt(byt.substring(6, 7)) == 1) count++;
                    if (count % 2 != previousParity) {
                        badBitIndex += i + 1;
                    }
                } else if (i == PARITY_SEQUENCE[2]) {
                    int count = 0;
                    if (Integer.parseInt(byt.substring(4, 5)) == 1) count++;
                    if (Integer.parseInt(byt.substring(5, 6)) == 1) count++;
                    if (Integer.parseInt(byt.substring(6, 7)) == 1) count++;
                    if (count % 2 != previousParity) {
                        badBitIndex += i + 1;
                    }
                }
            }
            badByte.replace(badBitIndex - 1, badBitIndex, badByte.charAt(badBitIndex - 1) == '1' ? "0" : "1");
            fixer.append(badByte);
        }

        StringBuilder decoder = new StringBuilder();
        String[] repairedDecodedArray = generateBinaryString(fixer.toString()).split(" ");
        for (String repairedByte : repairedDecodedArray) {
            decoder.append(repairedByte.charAt(2)).append(repairedByte, 4, 7);
        }
        try (FileOutputStream decode = new FileOutputStream(decoded)) {
            String receivedHex = generateHexString(String.join("", receive));
            String receivedBinary = generateBinaryString(String.join("", receive));
            String decodedBinary = generateBinaryString(String.join("", decoder.toString()));
            String correct = generateBinaryString(String.join("", fixer.toString()));
            int decodedRemainder = decoder.toString().length() % Byte.SIZE;
            String remover = String.join("", decoder.substring(0, (decoder.length() - decodedRemainder)));
            String remove = generateBinaryString(remover);
            String decodedHex = generateHexString(String.join("", decoder.toString()));
            StringBuilder text = new StringBuilder();
            for (String s : remove.split(" ")) {
                text.append((char) Integer.parseInt(s, 2));
            }
            System.out.printf("\n%s:" +
                            "\nhex view: %s" +
                            "\nbin view: %s" +
                            "\n" +
                            "\n%s:" +
                            "\ncorrect: %s" +
                            "\ndecode: %s" +
                            "\nhex view: %s" +
                            "\ntext view: %s" +
                            "\n", received.getName(), receivedHex, receivedBinary,
                    decoded.getName(),correct, decodedBinary, decodedHex, text);

            for (int i = 0; i < remover.length(); i += Byte.SIZE) {
                String newByte = remover.substring(i, i + Byte.SIZE);
                decode.write(Integer.parseInt(newByte, 2));
            }
        } catch (IOException e) {
            System.out.println("Write Error");
        }
    }

    protected static ArrayList<String> convertToBinary(File original) {
        ArrayList<String> converted = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(original)) {
            int ch = file.read();
            while (ch != -1) {
                String bin = Integer.toBinaryString(ch);
                while (bin.length() < Byte.SIZE) {
                    bin = String.format("0%s", bin);
                }
                converted.add(bin);
                ch = file.read();
            }
        } catch (IOException e) {
            System.out.println("Read Error");
        }
        return converted;
    }

    protected static String generateHexString(String original) {
        StringBuilder generate = new StringBuilder();
        int j = 0;
        for (int i = 0; i < original.length() / Byte.SIZE; i++) {
            StringBuilder toHex = new StringBuilder();
            int length = j + Byte.SIZE;
            for (; j < length; j++) {
                toHex.append(original.charAt(j));
            }
            String hex = Integer.toHexString(Integer.parseInt(toHex.toString(), 2)).toUpperCase();
            generate.append(hex.length() == 1 ? "0" + hex + " " : hex + " ");
        }
        return generate.toString().trim();
    }

    protected static String generateBinaryString(String original, boolean hiddenParity) {
        if (hiddenParity) {
            StringBuilder binaryString = new StringBuilder(original);
            for (int i = 0; i < binaryString.length(); i++) {
                for (int parity : PARITY_SEQUENCE) {
                    if (i % Byte.SIZE == parity) {
                        binaryString.replace(i, i + 1, ".");
                    }
                }
            }
            return generateBinaryString(binaryString.toString());
        } else {
            return generateBinaryString(original);
        }
    }

    protected static String generateBinaryString(String original) {
        StringBuilder generate = new StringBuilder(original);
        int length = generate.length() + generate.length() / Byte.SIZE;
        for (int i = Byte.SIZE; i < length; i += 9) {
            generate.insert(i, " ");
        }
        return generate.toString();
    }
}
