package com.ensifera.animosity.craftirc.example;

import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;

public class PluginExample extends JavaPlugin implements EndPoint {

        protected CraftIRC craftircHandle = null;
       
        public void onEnable() {
            Plugin checkplugin = this.getServer().getPluginManager().getPlugin("CraftIRC");
            if (checkplugin == null || !checkplugin.isEnabled()) {
                getServer().getPluginManager().disablePlugin(((org.bukkit.plugin.Plugin) (this)));
            } else {
                try {
                    craftircHandle = (CraftIRC) checkplugin;
                    craftircHandle.registerEndPoint("mytag", this);
                    RelayedMessage rm = craftircHandle.newMsg(this, null, "announcement");
                    rm.setField("message", "I'm aliiive!");
                    rm.post();
                } catch (ClassCastException ex) {
                    ex.printStackTrace();
                    getServer().getPluginManager().disablePlugin(((org.bukkit.plugin.Plugin) (this)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void onDisable() {
            if (craftircHandle != null) craftircHandle.unregisterEndPoint("mytag");
        }

        public Type getType() {
            return EndPoint.Type.MINECRAFT;
        }

        public void messageIn(RelayedMessage msg) {
            if (msg.getEvent() == "join")
                getServer().broadcastMessage(msg.getField("sender") + " joined da game!");
        }

        public boolean userMessageIn(String username, RelayedMessage msg) {
            return false;
        }

        public boolean adminMessageIn(RelayedMessage msg) {
            return false;
        }

        public List<String> listUsers() {
            return null;
        }

        public List<String> listDisplayUsers() {
            return null;
        }

    }