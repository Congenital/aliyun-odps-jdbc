/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package com.aliyun.odps.jdbc.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.aliyun.odps.Instance;
import com.aliyun.odps.Odps;
import com.aliyun.odps.OdpsException;
import com.aliyun.odps.task.SQLTask;
import com.aliyun.odps.utils.StringUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Utils {

  public static final String JDBC_USER_AGENT = "odps.idata.useragent";
  public static final String JDBCKey = "driver.version";
  public static final String SDKKey = "sdk.version";
  public static String JDBCVersion = "JDBC-Version:" + retrieveVersion(JDBCKey);
  public static String SDKVersion = "SDK-Version:" + retrieveVersion(SDKKey);

  public static List<String> getSchemaList(Odps odps, String sql) throws SQLException {
    return Arrays.asList(getRawResult(odps, sql).split("\n"));
  }

  private static String getRawResult(Odps odps, String sql) throws SQLException {
    try {
      Instance i = SQLTask.run(odps, sql);
      i.waitForSuccess();
      return i.getTaskResults().get("AnonymousSQLTask");
    } catch (OdpsException e) {
      throw new SQLException(e);
    }
  }

  // see http://stackoverflow.com/questions/3697449/retrieve-version-from-maven-pom-xml-in-code
  public static String retrieveVersion(String key) {
    Properties prop = new Properties();
    try {
      prop.load(Utils.class.getResourceAsStream("/version.properties"));
      return prop.getProperty(key);
    } catch (IOException e) {
      return "unknown";
    }
  }

  public static boolean matchPattern(String s, String pattern) {

    if (StringUtils.isNullOrEmpty(pattern)) {
      return true;
    }

    pattern = pattern.toLowerCase();
    s = s.toLowerCase();

    if (pattern.contains("%") || pattern.contains("_")) {
      // (?<!a)  looks 1 char behind and ensure not equal
      String
          wildcard =
          pattern.replaceAll("(?<!\\\\)%", "\\\\w*").replaceAll("(?<!\\\\)_", "\\\\w");

      // escape / and %
      wildcard = wildcard.replace("\\%", "%").replace("\\_", "_");

      if (!s.matches(wildcard)) {
        return false;
      }
    } else {
      if (!s.equals(pattern)) {
        return false;
      }
    }

    return true;
  }


  // return record count of sum(Outputs) in json summary
  // -1 if no Outputs
  public static int getSinkCountFromTaskSummary(String jsonSummary) {
    if (StringUtils.isNullOrEmpty(jsonSummary)) {
      return -1;
    }

    int ret = 0;
    try {
      JsonObject summary = new JsonParser().parse(jsonSummary).getAsJsonObject();
      JsonObject outputs = summary.getAsJsonObject("Outputs");
      if ("{}".equals(outputs.toString())) {
        return -1;
      }

      for (Map.Entry<String, JsonElement> entry : outputs.entrySet()) {
        ret += entry.getValue().getAsJsonArray().get(0).getAsInt();
      }
    } catch (Exception e) {
      // do nothing
      e.printStackTrace();
    }
    return ret;
  }

  public static String parseSetting(String sql, Properties properties) {
    if (StringUtils.isNullOrEmpty(sql)) {
      throw new IllegalArgumentException("Invalid query :" + sql);
    }

    sql = sql.trim();
    if (!sql.endsWith(";")) {
      sql += ";";
    }
    int index = 0;
    int end = 0;
    while ((end = sql.indexOf(';', index)) != -1) {
      String s = sql.substring(index, end);
      if (s.toUpperCase().matches("(?i)^(\\s*)(SET)(\\s+)(.*)=(.*);?(\\s*)$")) {
        // handle one setting
        int i = s.toLowerCase().indexOf("set");
        String pairString = s.substring(i + 3);
        String[] pair = pairString.split("=");
        properties.put(pair[0].trim(), pair[1].trim());
        index = end + 1;
      } else {
        // break if there is no settings before
        break;
      }
    }
    if (index >= sql.length()) {
      // only settings, no query behind
      return null;
    } else {
      // trim setting before query
      return sql.substring(index).trim();
    }
  }
}
