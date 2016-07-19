/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

import net.quux00.simplecsv.CsvParser;
import net.quux00.simplecsv.CsvParserBuilder;
import net.quux00.simplecsv.CsvReader;
import net.quux00.simplecsv.CsvReaderBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class SimulationParser {

    private static final String OK = "OK";
    private static final String REQUEST = "REQUEST";
    private static final String RUN = "RUN";
    private static final String USER = "USER";
    private static final String START = "START";
    private static final String END = "END";
    private static final String GZ = "gz";
    private final File file;
    private final Float apdexT;

    public SimulationParser(File file, Float apdexT) {
        System.out.println("file: " + file.getName());
        System.out.println("apdexT: " + apdexT);
        this.file = file;
        this.apdexT = apdexT;
    }

    public SimulationParser(File file) {
        this.file = file;
        this.apdexT = null;
    }

    public SimulationContext parse() throws IOException {
        SimulationContext ret = new SimulationContext(file.getAbsolutePath(), apdexT);
        CsvParser p = new CsvParserBuilder().trimWhitespace(true).allowUnbalancedQuotes(true).separator('\t').build();
        CsvReader reader = new CsvReaderBuilder(getReaderFor(file)).csvParser(p).build();
        List<String> line;
        String name;
        String scenario;
        long start, end;
        boolean success;
        while ((line = reader.readNext()) != null) {
            System.out.println("line: " + line + " line.size(): " + line.size());
            if (line.size() <= 2) {
                invalidFile();
            }
            scenario = line.get(1); //0
            System.out.println("scenario: " + scenario + " line.get(2): " + line.get(2));
            switch (line.get(0)) { //2
                case RUN:
                    String version = line.get(6);//5
                    System.out.println("version: " + version);
                    if (!version.startsWith("2.")) {
                        System.out.println("Yikes!");
                        return invalidFile();
                    }
                    ret.setSimulationName(line.get(3));//1
                    ret.setStart(Long.parseLong(line.get(4))); // 3
                    System.out.println("RUN: " + line.get(0) +
                            " version: " + line.get(6) + " SimulationName: " + line.get(3) +
                            " Start: " + line.get(4));
                    break;
                case REQUEST:
                    name = line.get(1);//4
                    start = Long.parseLong(line.get(5));//6
                    end = Long.parseLong(line.get(6));//8
                    success = OK.equals(line.get(7));//9
                    ret.addRequest(scenario, name, start, end, success);
                    System.out.println("REQUEST: " + line.get(2) + "name: " + line.get(1) +
                            " start: " + line.get(5) + " end: " + line.get(6) +
                            " success: " + line.get(7));
                    break;
                case USER:
                    System.out.println("USER line.get(2): " + line.get(2));
                    switch (line.get(3)) {//3
                        case START:
                            ret.addUser(scenario);
                            System.out.println("USER START: " + line.get(2) + " switch on: " + line.get(3));
                            break;
                        case END:
                            ret.endUser(scenario);
                            System.out.println("USER END: " + line.get(2) + " switch on: " + line.get(3));
                            break;
                    }
                    break;
            }
        }
        System.out.println("SimulationContext ret: " + ret);
        ret.computeStat();
        return ret;
    }

    private SimulationContext invalidFile() {
        throw new IllegalArgumentException(String.format("Invalid simulation file: %s expecting " +
                "Gatling 2.x format", file.getAbsolutePath()));
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private Reader getReaderFor(File file) throws IOException {
        if (GZ.equals(getFileExtension(file))) {
            InputStream fileStream = new FileInputStream(file);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            return new InputStreamReader(gzipStream, "UTF-8");
        }
        return new FileReader(file);
    }

}
