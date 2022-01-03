import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    static int clockCycle;
    static int PC = 0;
    static int PCpipline = 0;
    static int PCpipline1 = 0;
    static int[] mainMemory = new int[2048];
    static int[] registerFile = new int[32];
    static int tempRegister = 0;
    static int tempRegisterPipline = 0;
    static int fixedCount = 2;
    static int instruction = 0;
    static int opcode = 0;
    static int opcodePipline = 0;
    static int opcodePipline2 = 0;
    static int r1 = 0;
    static int r1Pipline = 0;
    static int r1Pipline2 = 0;
    static int r2 = 0;
    static int r3 = 0;
    static int shamt = 0;
    static int shamtPipeline = 0;
    static int immediate = 0;
    static int immediatePipline = 0;
    static int address = 0;
    static int addressPipeline = 0;
    static int readReg1 = 0;
    static int readReg2 = 0;
    static int readReg3 = 0;
    static int readReg1Pipline = 0;
    static int readReg2Pipline = 0;
    static int readReg3Pipline = 0;
    static int fetchCount = 0;
    static int decodeCount = 0;
    static int executeCount = 0;
    static int memoryCount = 0;
    static int writeCount = 0;
    static boolean decodeFlag = false;
    static boolean executeFlag = false;
    static boolean jumpFlag = false;
    static int blockDecode = 0;
    static int blockExecute = 0;
    static int blockMemory = 0;
    static int blockWrite = 0;
    static int stop = 0;
    static int tempPC = 0;
    static int jumpInt = 0;

    public static void main(String[] args) throws IOException {
        parser(args[0]);
        pipeline();
        System.out.println("Program End");
        for (int i = 0; i < registerFile.length; i++) {
            System.out.println("R" + i + ": " + registerFile[i]);
        }
        for (int i = 0; i < mainMemory.length; i++) {
            System.out.println("Memory Position " + i + ": " + mainMemory[i]);
        }
    }

    // parse
    public static void parser(String programName) throws IOException {
        String path = "src/resources/" + programName + ".txt";
        File file = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String[] s;
        fixedCount = (int) br.lines().count();
        br.close();
        br = new BufferedReader(new FileReader(file));
        int j = 0;
        String str;
        while ((str = br.readLine()) != null) {
            s = str.split(" ");
            String binaryInstruction = "";
            switch (s[0]) {
                case "ADD":
                    binaryInstruction = "0000";
                    break;
                case "SUB":
                    binaryInstruction = "0001";
                    break;
                case "MULI":
                    binaryInstruction = "0010";
                    break;
                case "ADDI":
                    binaryInstruction = "0011";
                    break;
                case "BNE":
                    binaryInstruction = "0100";
                    break;
                case "ANDI":
                    binaryInstruction = "0101";
                    break;
                case "ORI":
                    binaryInstruction = "0110";
                    break;
                case "J":
                    binaryInstruction = "0111";
                    break;
                case "SLL":
                    binaryInstruction = "1000";
                    break;
                case "SRL":
                    binaryInstruction = "1001";
                    break;
                case "LW":
                    binaryInstruction = "1010";
                    break;
                case "SW":
                    binaryInstruction = "1011";
                    break;
            }
            int R1;
            int R2;
            int R3;
            int SHAMT;
            int ADDRESS;
            int IMM;

            if (!s[0].equals("J")) {
                R1 = Integer.parseInt(s[1].replace("R", ""));
                for (int i = Integer.toBinaryString(R1).length(); i < 5; i++) {
                    binaryInstruction += "0";
                }
                binaryInstruction += Integer.toBinaryString(R1);
                R2 = Integer.parseInt(s[2].replace("R", ""));
                for (int i = Integer.toBinaryString(R2).length(); i < 5; i++) {
                    binaryInstruction += "0";
                }
                binaryInstruction += Integer.toBinaryString(R2);
                if (s[0].equals("SUB") || s[0].equals("ADD")) {
                    R3 = Integer.parseInt(s[3].replace("R", ""));
                    for (int i = Integer.toBinaryString(R3).length(); i < 5; i++) {
                        binaryInstruction += "0";
                    }
                    binaryInstruction += Integer.toBinaryString(R3);
                    binaryInstruction += "0000000000000";
                } else if (s[0].equals("SLL") || s[0].equals("SRL")) {
                    binaryInstruction += "00000";
                    SHAMT = Integer.parseInt(s[3]);
                    for (int i = Integer.toBinaryString(SHAMT).length(); i < 13; i++) {
                        binaryInstruction += "0";
                    }
                    binaryInstruction += Integer.toBinaryString(SHAMT);
                } else {
                    IMM = Integer.parseInt(s[3]);
                    if(IMM >= 0){
                        for (int i = Integer.toBinaryString(IMM).length(); i < 18; i++) {
                            binaryInstruction += "0";
                        }
                        binaryInstruction += Integer.toBinaryString(IMM);
                    }
                    else
                        binaryInstruction += Integer.toBinaryString(IMM).substring(14);
                }

            } else {
                ADDRESS = Integer.parseInt(s[1]);
                for (int i = Integer.toBinaryString(ADDRESS).length(); i < 28; i++) {
                    binaryInstruction += "0";
                }
                binaryInstruction += Integer.toBinaryString(ADDRESS);
            }
            mainMemory[j++] = (int) Long.parseLong(binaryInstruction, 2);
        }
    }

    // pipeline
    public static void pipeline() {
        for (int i = 1; stop < 7; i++) {
            if(PC < fixedCount -1)
                stop = 0;
            if (PC >= fixedCount - 1) {
                stop++;
            }
            System.out.println("Cycle No. " + i);
            // writeBack
            if (i >= 7 && i % 2 != 0 && (blockWrite <= 0 || blockWrite == 6) && stop <= 7) {
                writeBack();
                if (jumpFlag && 0 == jumpInt--) {
                    jumpFlag = false;
                }
            }
            blockWrite--;
            // memory
            if (i >= 6 && i % 2 == 0 && blockMemory <= 0 && stop <= 6) {
                if (jumpFlag) {
                    PC = tempPC;
                    blockDecode = 1;
                    blockExecute = 3;
                    blockMemory = 6;
                    blockWrite = 6;
                }
                memory();
            }
            blockMemory--;
            if (executeFlag) {
                System.out.println("Instruction " + executeCount + " is at the execute stage." + "\nINPUT: opcode = "
                        + opcodePipline + " PC = " + PCpipline1 + " shamt= " + shamtPipeline + " immediate = "
                        + immediatePipline + " address = " + addressPipeline + "\nData in r1 = " + readReg1Pipline
                        + " Data in r2 = " + readReg2Pipline + " Data in r3 = " + readReg3Pipline);
                if (opcodePipline == 4 || opcodePipline == 7)
                    System.out.println("OUTPUT: PC = " + tempPC + "\n");
                else if (opcodePipline != 10 && opcodePipline != 11) {
                    System.out.println("OUTPUT: ALU Result = " + tempRegister + "\n");
                } else {
                    System.out.println();
                }
                executeFlag = false;
            }
            // execute
            if (i >= 4 && i % 2 == 0 && blockExecute <= 0 && stop <= 4) {
                execute();
                System.out.println("Instruction " + executeCount + " is at the execute stage." + "\nINPUT: opcode = "
                        + opcode + " PC = " + PCpipline1 + " shamt= " + shamtPipeline + " immediate = "
                        + immediatePipline + " address = " + addressPipeline + "\nData in r1 = " + readReg1
                        + " Data in r2 = " + readReg2 + " Data in r3 = " + readReg3Pipline);
                if (opcode == 4 || opcode == 7)
                    System.out.println("OUTPUT: PC = " + tempPC + "\n");
                else if (opcode != 10 && opcode != 11) {
                    System.out.println("OUTPUT: ALU Result = " + tempRegister + "\n");
                } else {
                    System.out.println();
                }
                executeFlag = true;
            }
            blockExecute--;
            // decode
            if (decodeFlag) {
                System.out.println("Instruction " + decodeCount + " is at the decode stage.\nINPUT: instruction = "
                        + Integer.toBinaryString(instruction) + "\nOUTPUT: opcode = " + opcode + " r1 = " + r1
                        + " r2 = " + r2 + " r3 = " + r3 + " shamt= " + shamt + " immediate = " + immediate
                        + " address = " + address + "\nData in r1 = " + readReg1 + " Data in r2 = " + readReg2
                        + " Data in r3 = " + readReg3 + "\n");
                decodeFlag = false;
            }
            if (i % 2 == 0 && blockDecode <= 0 && stop <= 2) {
                decode();
                System.out.println("Instruction " + decodeCount + " is at the decode stage.\nINPUT: instruction = "
                        + Integer.toBinaryString(instruction) + "\nOUTPUT: opcode = " + opcode + " r1 = " + r1
                        + " r2 = " + r2 + " r3 = " + r3 + " shamt= " + shamt + " immediate = " + immediate
                        + " address = " + address + "\nData in r1 = " + readReg1 + " Data in r2 = " + readReg2
                        + " Data in r3 = " + readReg3 + "\n");
                decodeFlag = true;
            }
            blockDecode--;
            // fetch
            if (i % 2 != 0 && PC <= fixedCount - 1) {
                fetch();
            }
            System.out.println("\n\n");

        }
    }

    // write back
    public static void writeBack() {
        System.out.println("Instruction " + memoryCount + " is at the write back stage.");

        if (r1Pipline2 != 0)
            switch (opcodePipline2) {
                case 0, 1, 2, 3, 5, 6, 8, 9, 10:
                    registerFile[r1Pipline2] = tempRegisterPipline;
                    System.out.println("INPUT: Destination Register = " + r1Pipline2 + " Write Back Value = "
                            + tempRegisterPipline);
                    System.out.println("Register R" + r1Pipline2 + " was modified to contain " + tempRegisterPipline
                            + " at the write back stage.");
            }
        System.out.println();
    }

    // memory
    public static void memory() {
        switch (opcodePipline) {
            case 10:
                // load
                tempRegister = mainMemory[readReg2Pipline + immediatePipline];
                break;
            case 11:
                // store
                mainMemory[readReg2Pipline + immediatePipline] = readReg1Pipline;
                break;
        }
        tempRegisterPipline = tempRegister;
        opcodePipline2 = opcodePipline;
        r1Pipline2 = r1Pipline;
        memoryCount = executeCount;
        System.out.println("Instruction " + memoryCount + " is at the memory stage." + "\nINPUT: opcode = "
                + opcodePipline + " immediate = " + immediatePipline + "\nData in r1 = " + readReg1Pipline
                + " Data in r2 = " + readReg2Pipline);
        if (opcodePipline == 10) {
            System.out.println("OUTPUT: Write Back Value = " + tempRegisterPipline);
        }
        if (opcodePipline == 11) {
            System.out.println("Memory Position " + (readReg2Pipline + immediatePipline) + " was modified to contain "
                    + readReg1Pipline + " at the memory stage");
        }
        System.out.println();

    }

    // execute
    public static void execute() {
        switch (opcode) {
            case 0:
                // add
                tempRegister = readReg2 + readReg3;
                break;
            case 1:
                // subtract
                tempRegister = readReg2 - readReg3;
                break;
            case 2:
                // multiply immediate
                tempRegister = readReg2 * immediate;
                break;
            case 3:
                // add immediate
                tempRegister = readReg2 + immediate;
                break;
            case 4:
                // branch if not equal
                if (readReg1 != readReg2) {
                    tempPC = PCpipline + 1 + immediate;
                    jumpFlag = true;
                    jumpInt = 1;
                }
                break;
            case 5:
                // and immediate
                tempRegister = readReg2 & immediate;
                break;
            case 6:
                // or immediate
                tempRegister = readReg2 | immediate;
                break;
            case 7:
                // jump
                tempPC = address;
                jumpFlag = true;
                jumpInt = 1;
                break;
            case 8:
                // shift left logical
                tempRegister = readReg2 << shamt;
                break;
            case 9:
                // shift right logical
                tempRegister = readReg2 >>> shamt;
                break;
        }
        readReg1Pipline = readReg1;
        readReg2Pipline = readReg2;
        readReg3Pipline = readReg3;
        r1Pipline = r1;
        opcodePipline = opcode;
        shamtPipeline = shamt;
        addressPipeline = address;
        executeCount = decodeCount;
        PCpipline1 = PCpipline;
        immediatePipline = immediate;
    }

    // decode
    public static void decode() {
        opcode = (instruction & 0b11110000000000000000000000000000) >>> 28;
        r1 = (instruction & 0b00001111100000000000000000000000) >>> 23;
        r2 = (instruction & 0b00000000011111000000000000000000) >>> 18;
        r3 = (instruction & 0b00000000000000111110000000000000) >>> 13;
        shamt = instruction & 0b00000000000000000001111111111111;
        immediate = ((instruction & 0b00000000000000111111111111111111) << 14) >> 14;
        address = instruction & 0b00001111111111111111111111111111;
        readReg1 = registerFile[r1];
        readReg2 = registerFile[r2];
        readReg3 = registerFile[r3];
        decodeCount = fetchCount;
        PCpipline = PC;
        PC++;
    }

    // fetch
    public static void fetch() {
        fetchCount = PC + 1;
        instruction = mainMemory[PC];
        System.out.println("Instruction " + fetchCount + " is at the fetch stage.\nINPUT: PC = "
                + Integer.toBinaryString(PC) + "\nOUTPUT: instruction = " + Integer.toBinaryString(instruction) + "\n");
    }
}