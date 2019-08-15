package com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.scoutzknifez.ranchocloverinventorymanager.Interfaces.Filterable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonPropertyOrder({
        "id",
        "name",
        "showInReporting"
})

@Getter @Setter @AllArgsConstructor
public class CloverTag implements Serializable, Filterable {
    @JsonProperty(value="id") private String id;
    @JsonProperty(value="name") private String name;
    @JsonProperty(value="showInReporting") private boolean showInReporting = true;

    public CloverTag() {}

    public CloverTag(String name) {
        setName(name);
    }

    public boolean containsFilter(String string) {
        string = string.trim();
        return getId().equalsIgnoreCase(string) || getName().equalsIgnoreCase(string);
    }

    @Override
    public String toString() {
        return "<| Clover Tag |> \n" +
                "ID: " + getId() + "\n" +
                "Name: " + getName() + "\n" +
                "Show in Reporting: " + isShowInReporting() + "\n" +
                "----------------";
    }
}