package com.datasift.client.mock.datasift;

import com.datasift.client.managedsource.ManagedSource;
import com.datasift.client.managedsource.ManagedSourceLog;
import com.datasift.client.managedsource.sources.DataSource;
import com.datasift.client.managedsource.sources.FacebookPage;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.higgs.core.method;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.params.FormParam;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by agnieszka on 17/01/2014.
 */
@method("/v1.1/source")
public class MockManagedSourcesApi {
    Map<String, String> headers = new HashMap<>();
    private Map<String, Object> streams = new HashMap<>();
    private String name;
    private String id;
    private ManagedSource m_id;
    private Map<String, Object> parameters;
    private Set auth_set = new HashSet();
    private Map<String, Object> auth = new HashMap<>();
    private Set resource_set = new HashSet();
    private Map<String, Object> resource = new HashMap<>();
    private DateTime created_at = DateTime.now();
    private int count = new Random().nextInt();
    private int page = new Random().nextInt();
    private int pages = new Random().nextInt();
    private int per_page = new Random().nextInt();
    private List<ManagedSourceLog.LogMessage> entries;
    protected String sourceType;
    protected Set<ManagedSource.ResourceParams> resources = new NonBlockingHashSet<ManagedSource.ResourceParams>();
    protected long createdAt;
    private String identityId;
    private String sourceId;
    private String status;


    @method("create")
    public Map<String, Object> create() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("source_type", sourceType);
        map.put("parameters", parameters);

        auth.put("identity_id", identityId);
        auth.put("source_id", sourceId);
        auth.put("status", status);
        auth_set.add(auth);
        map.put("auth", auth_set);
        resource.put("identity_id", identityId);
        resource.put("source_id", sourceId);
        resource.put("status", status);
        resource_set.add(resource);
        map.put("resource", resource_set);
        map.put("created_at", created_at);
        map.put("id", id);
        map.put("status", status);

        return map;
    }

    @method("update")
    public Map<String, Object> update() {
        Map<String, Object> map = new HashMap<>();

        return map;
    }

    @method("delete")
    public Map<String, Object> delete() {
        Map<String, Object> map = new HashMap<>();

        return map;
    }

    @method("log")
    public Map<String, Object> log() {
        Map<String, Object> map = new HashMap<>();

        return map;
    }

    @method("get")
    public Map<String, Object> get() {
        Map<String, Object> map = new HashMap<>();

        return map;
    }

    @method("stop")
    public Map<String, Object> stop() {
        Map<String, Object> map = new HashMap<>();



        return map;
    }

    @method("start")
    public Map<String, Object> start() {
        Map<String, Object> map = new HashMap<>();

        return map;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setStreams(Map<String, Object> streams) {
        this.streams = streams;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setM_id(ManagedSource m_id) {
        this.m_id = m_id;
    }


    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setAuth_set(Set auth_set) {
        this.auth_set = auth_set;
    }

    public void setAuth(Map<String, Object> auth) {
        this.auth = auth;
    }

    public void setResource_set(Set resource_set) {
        this.resource_set = resource_set;
    }

    public void setResource(Map<String, Object> resource) {
        this.resource = resource;
    }

    public void setCreated_at(DateTime created_at) {
        this.created_at = created_at;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public void setPer_page(int per_page) {
        this.per_page = per_page;
    }

    public void setEntries(List<ManagedSourceLog.LogMessage> entries) {
        this.entries = entries;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setResources(Set<ManagedSource.ResourceParams> resources) {
        this.resources = resources;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}