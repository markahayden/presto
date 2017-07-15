/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.cli;

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

public class TableauConfig
{
    private String host;
    private String username;
    private String password;
    private String siteID;
    private String project;
    private String extractName;
    private String datasource;
    private boolean overwrite;

    public String getHost()
    {
        return host;
    }

    @Config("com.tableausoftware.server.ServerConnection.host")
    @ConfigDescription("Set hostname for tableau server")
    public TableauConfig setHost(String host)
    {
        this.host = host;
        return this;
    }

    public String getUsername()
    {
        return username;
    }

    @Config("com.tableausoftware.server.ServerConnection.username")
    @ConfigDescription("Set username for tableau server")
    public TableauConfig setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public String getPassword()
    {
        return password;
    }

    @Config("com.tableausoftware.server.ServerConnection.password")
    @ConfigDescription("Set password for tableau server")
    public TableauConfig setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public String getSiteID()
    {
        return siteID;
    }

    @Config("com.tableausoftware.server.ServerConnection.siteID")
    @ConfigDescription("Set siteID for tableau server")
    public TableauConfig setSiteID(String siteID)
    {
        this.siteID = siteID;
        return this;
    }

    public String getProject()
    {
        return project;
    }

    @Config("com.tableausoftware.server.ServerConnection.project")
    @ConfigDescription("Project to publish to on tableau server")
    public TableauConfig setProject(String project)
    {
        this.project = project;
        return this;
    }

    public String getExtractName()
    {
        return extractName;
    }

    @Config("com.tableausoftware.server.ServerConnection.extractName")
    @ConfigDescription("Set local extract name")
    public TableauConfig setExtractName(String extractName)
    {
        this.extractName = extractName;
        return this;
    }

    public String getDatasource()
    {
        return datasource;
    }

    @Config("com.tableausoftware.server.ServerConnection.datasourceName")
    @ConfigDescription("Set data source name for tableau server")
    public TableauConfig setDatasource(String datasource)
    {
        this.datasource = datasource;
        return this;
    }

    public boolean getOverwrite()
    {
        return overwrite;
    }

    @Config("com.tableausoftware.server.ServerConnection.overwrite")
    @ConfigDescription("Overwrite existing extract on tableau server")
    public TableauConfig setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
        return this;
    }
}
