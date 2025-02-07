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

package com.aliyun.odps.jdbc.utils.transformer.to.odps;

import java.sql.SQLException;
import java.time.ZoneId;

public class ToOdpsDateTransformer extends AbstractToOdpsTransformer {

  @Override
  public Object transform(Object o, String charset) throws SQLException {
    if (o == null) {
      return null;
    }

    if (java.sql.Date.class.isInstance(o)) {
      return new java.util.Date(((java.sql.Date) o).getTime()).toInstant().atZone(ZoneId.systemDefault());
    } else if (java.util.Date.class.isInstance(o)) {
      return ((java.util.Date) o).toInstant().atZone(ZoneId.systemDefault());
    } else {
      String errorMsg = getInvalidTransformationErrorMsg(o.getClass(), java.sql.Date.class);
      throw new SQLException(errorMsg);
    }
  }
}