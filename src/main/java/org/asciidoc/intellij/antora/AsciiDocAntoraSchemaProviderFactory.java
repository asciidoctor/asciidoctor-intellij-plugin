package org.asciidoc.intellij.antora;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class AsciiDocAntoraSchemaProviderFactory implements JsonSchemaProviderFactory {
  @NotNull
  @Override
  public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
    return Arrays.asList(new AntoraComponentSchemaFileProvider(), new AntoraPlaybookSchemaFileProvider());
  }

  public abstract static class BaseJsonSchemaFileProvider implements JsonSchemaFileProvider {
    private VirtualFile resourceFile;

    @Override
    public final @Nullable VirtualFile getSchemaFile() {
      // in order for the UI to attach the name of the schema to the file, the method will need to return the same
      // file on each call
      if (resourceFile == null) {
        synchronized (this) {
          if (resourceFile == null) {
            resourceFile = getResourceFile(getResourceName());
          }
        }
      }
      return resourceFile;
    }

    protected abstract String getResourceName();
  }

  public static class AntoraComponentSchemaFileProvider extends BaseJsonSchemaFileProvider {

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().equals("antora.yml");
    }

    @NotNull
    @Override
    public String getName() {
      return "Antora Component Schema";
    }

    @NotNull
    @Override
    public SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }

    @Override
    protected String getResourceName() {
      return "/jsonSchemas/antoraComponentSchema.json";
    }
  }

  public static class AntoraPlaybookSchemaFileProvider extends BaseJsonSchemaFileProvider {

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().endsWith(".yml") && file.getName().contains("antora") && file.getName().contains("playbook");
    }

    @NotNull
    @Override
    public String getName() {
      return "Antora Playbook Schema";
    }

    @NotNull
    @Override
    public SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }

    @Override
    protected String getResourceName() {
      return "/jsonSchemas/antoraPlaybookSchema.json";
    }
  }

  public static VirtualFile getResourceFile(String resourcePath) {
    InputStream resourceAsStream = null;
    try {
      resourceAsStream = AsciiDocAntoraSchemaProviderFactory.class.getResourceAsStream(resourcePath);
      if (resourceAsStream != null) {
        String name = resourcePath;
        int index = name.lastIndexOf('/');
        if (index != -1) {
          name = resourcePath.substring(index + 1);
        }
        return new LightVirtualFile(name, JsonFileType.INSTANCE, IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8),
          StandardCharsets.UTF_8, LocalTimeCounter.currentTime());
      } else {
        LOG.error("resource not found: " + resourcePath);
      }
    } catch (IOException e) {
      LOG.error("unable to read resource: " + resourcePath, e);
    } finally {
      if (resourceAsStream != null) {
        try {
          resourceAsStream.close();
        } catch (IOException e) {
          LOG.error("unable to close resource: " + resourcePath, e);
        }
      }
    }
    return null;
  }


}
