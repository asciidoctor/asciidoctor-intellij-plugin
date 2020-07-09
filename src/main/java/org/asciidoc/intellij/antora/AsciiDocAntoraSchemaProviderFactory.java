package org.asciidoc.intellij.antora;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class AsciiDocAntoraSchemaProviderFactory implements JsonSchemaProviderFactory {
  @NotNull
  @Override
  public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
    return Arrays.asList(new AntoraComponentSchemaFileProvider(), new AntoraPlaybookSchemaFileProvider());
  }

  public static class AntoraComponentSchemaFileProvider implements JsonSchemaFileProvider {

    private final NullableLazyValue<VirtualFile> mySchemaFile;

    public AntoraComponentSchemaFileProvider() {
      mySchemaFile = NullableLazyValue.createValue(() -> JsonSchemaProviderFactory.getResourceFile(AsciiDocAntoraSchemaProviderFactory.class, "/jsonSchemas/antoraComponentSchema.json"));
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().equals("antora.yml");
    }

    @NotNull
    @Override
    public String getName() {
      return "Antora Component Schema";
    }

    @Nullable
    @Override
    public VirtualFile getSchemaFile() {
      return mySchemaFile.getValue();
    }

    @NotNull
    @Override
    public SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }
  }

  public static class AntoraPlaybookSchemaFileProvider implements JsonSchemaFileProvider {

    private final NullableLazyValue<VirtualFile> mySchemaFile;

    public AntoraPlaybookSchemaFileProvider() {
      mySchemaFile = NullableLazyValue.createValue(() -> JsonSchemaProviderFactory.getResourceFile(AsciiDocAntoraSchemaProviderFactory.class, "/jsonSchemas/antoraPlaybookSchema.json"));
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().endsWith(".yml") && file.getName().contains("antora") && file.getName().contains("playbook");
    }

    @NotNull
    @Override
    public String getName() {
      return "Antora Playbook Schema";
    }

    @Nullable
    @Override
    public VirtualFile getSchemaFile() {
      return mySchemaFile.getValue();
    }

    @NotNull
    @Override
    public SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }
  }

}
