package org.asciidoc.intellij.antora;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AsciiDocAntoraSchemaProviderFactory implements JsonSchemaProviderFactory {
  @NotNull
  @Override
  public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
    return Collections.singletonList(new MyJsonSchemaFileProvider());
  }

  public static class MyJsonSchemaFileProvider implements JsonSchemaFileProvider {

    private final NullableLazyValue<VirtualFile> mySchemaFile;

    public MyJsonSchemaFileProvider() {
      mySchemaFile = NullableLazyValue.createValue(() -> JsonSchemaProviderFactory.getResourceFile(AsciiDocAntoraSchemaProviderFactory.class, "/jsonSchemas/antoraSchema.json"));
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().equals("antora.yml");
    }

    @NotNull
    @Override
    public String getName() {
      return "Antora Schema";
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
