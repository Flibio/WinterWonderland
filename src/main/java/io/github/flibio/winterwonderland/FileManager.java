/*
 * This file is part of WinterWonderland, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2016 Flibio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.flibio.winterwonderland;

import com.typesafe.config.ConfigException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;

import java.io.File;
import java.io.IOException;

public class FileManager {

    // Declare FileType enumerator
    public enum FileType {
        CONFIGURATION,
        DATA
    }

    private Logger logger;
    private ConfigurationNode configRoot;
    private ConfigurationNode dataRoot;

    public FileManager(Logger logger) {
        this.logger = logger;
    }

    public void testDefault(String path, Object value) {
        if (configRoot != null) {
            // Check if the configuration file doesn't contain the path
            if (configRoot.getNode((Object[]) path.split("\\.")).getValue() == null) {
                // Set the path to the default value
                configRoot.getNode((Object[]) path.split("\\.")).setValue(value);
                saveFile(FileType.CONFIGURATION, configRoot);
            }
        }
    }

    public String getConfigValue(String path) {
        if (configRoot != null) {
            // Check if the configuration file contains the path
            if (configRoot.getNode((Object[]) path.split("\\.")).getValue() != null) {
                // Get the value and return it
                return configRoot.getNode((Object[]) path.split("\\.")).getString();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public void generateFolder(String path) {
        File folder = new File(path);
        try {
            if (!folder.exists()) {
                logger.info(path + " not found, generating...");
                if (folder.mkdir()) {
                    logger.info("Successfully generated " + path);
                } else {
                    logger.warn("Error generating " + path);
                }

            }
        } catch (Exception e) {
            logger.warn("Error generating " + path + ": ");
            logger.warn(e.getMessage());
        }
    }

    public void generateFile(String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                logger.info(path + " not found, generating...");
                try {
                    file.createNewFile();
                    logger.info("Successfully generated " + path);
                } catch (IOException e) {
                    logger.error("Error generating " + path);
                    logger.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Error generating " + path + ": ");
            logger.warn(e.getMessage());
        }
    }

    public void loadFile(FileType file) {
        String fileName = "";
        switch (file) {
            case CONFIGURATION:
                fileName = "config.conf";
                break;

            case DATA:
                fileName = "data.conf";
                break;
        }

        ConfigurationLoader<?> manager = HoconConfigurationLoader.builder().setFile(new File("config/WinterWonderland/" + fileName)).build();
        ConfigurationNode root;
        try {
            root = manager.load();
        } catch (IOException e) {
            logger.error("Error loading " + fileName + "!");
            logger.error(e.getMessage());
            return;
        } catch (ConfigException e) {
            logger.error("Error loading " + fileName + "!");
            logger.error("Did you edit something wrong? For a blank value use double quotes.");
            logger.error(e.getMessage());
            return;
        }

        switch (file) {
            case CONFIGURATION:
                configRoot = root;
                break;

            case DATA:
                dataRoot = root;
                break;
        }
    }

    public ConfigurationNode getFile(FileType file) {
        switch (file) {
            case CONFIGURATION:
                return configRoot;

            case DATA:
                return dataRoot;
            default:
                return null;
        }
    }

    public void saveFile(FileType file, ConfigurationNode root) {
        Sponge.getScheduler().createTaskBuilder().execute(c -> {
            String fileName = "";
            switch (file) {
                case CONFIGURATION:
                    fileName = "config.conf";
                    break;

                case DATA:
                    fileName = "data.conf";
                    break;
            }
            ConfigurationLoader<?> manager = HoconConfigurationLoader.builder().setFile(new File("config/WinterWonderland/" + fileName)).build();

            try {
                manager.save(root);
            } catch (IOException e) {
                logger.error("Error saving " + fileName + "!");
                logger.error(e.getMessage());
            }
        }).async().submit(Main.access);
    }

}
