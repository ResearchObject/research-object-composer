package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonPropertyOrder({ "@context", "@graph" })
public class ROCrateMetadata {

    @JsonPropertyOrder({ "@id", "@type" })
    class Author {
        private String name;
        private String identifier;
        private String email;
        private String familyName;
        private String givenName;

        public Author() { }

        public Author(String name) {
            this.name = name;
        }

        @JsonProperty("@type")
        public String getType() {
            return "Person";
        }

        @JsonProperty("@id")
        public String getId() {
            if (this.getIdentifier() != null) {
                return this.getIdentifier();
            } else {
                String id;
                try {
                    id = URLEncoder.encode(this.getName().replace(' ', '_'), StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    id = UUID.randomUUID().toString();
                }

                return "#" + id;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }
    }

    @JsonPropertyOrder({ "@id", "@type" })
    class File {
        private String path;
        private String name;
        private URL contentUrl;
        private long contentSize;

        public File() { }

        public File(String path, String name, URL contentUrl, long contentSize) {
            this.path = path;
            this.name = name;
            this.contentUrl = contentUrl;
            this.contentSize = contentSize;
        }

        @JsonProperty("@type")
        public String getType() {
            return "File";
        }

        @JsonProperty("@id")
        public String getId() {
            return this.getPath();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public URL getContentUrl() {
            return contentUrl;
        }

        public void setContentUrl(URL contentUrl) {
            this.contentUrl = contentUrl;
        }

        public long getContentSize() {
            return contentSize;
        }

        public void setContentSize(long contentSize) {
            this.contentSize = contentSize;
        }
    }

    class Reference {
        private String id;

        public Reference(String id) {
            this.id = id;
        }

        @JsonProperty("@id")
        public String getId() {
            return this.id;
        }
    }

    @JsonPropertyOrder({ "@type", "path" })
    class SelfMetadata {
        private String name;
        private String description;
        private String datePublished;
        private List<Reference> fileRefs;
        private List<Reference> authorRefs;

        public SelfMetadata() {
            this.fileRefs = new ArrayList<>();
            this.authorRefs = new ArrayList<>();
        }

        @JsonProperty("@type")
        public String getType() {
            return "Dataset";
        }

        @JsonProperty("path")
        public String getPath() {
            return "./";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDatePublished() {
            return datePublished;
        }

        public void setDatePublished(String datePublished) {
            this.datePublished = datePublished;
        }

        @JsonProperty("hasPart")
        public List<Reference> getFileRefs() {
            return fileRefs;
        }

        public void setFileRefs(List<Reference> fileRefs) {
            this.fileRefs = fileRefs;
        }

        public void addFileRef(Reference fileRef) {
            this.fileRefs.add(fileRef);
        }

        @JsonProperty("creator")
        public List<Reference> getAuthorRefs() {
            return authorRefs;
        }

        public void setAuthorRefs(List<Reference> authorRefs) {
            this.authorRefs = authorRefs;
        }

        public void addAuthorRef(Reference authorRef) {
            this.authorRefs.add(authorRef);
        }
    }

    private SelfMetadata selfMetadata;
    private List<File> files;
    private List<Author> authors;

    private final static String CONTEXT = "https://w3id.org/ro/crate/0.2-DRAFT/context";

    public ROCrateMetadata() {
        this.selfMetadata = new SelfMetadata();
        this.files = new ArrayList<>();
        this.authors = new ArrayList<>();
    }

    @JsonProperty("@context")
    public String getContext() {
        return CONTEXT;
    }

    @JsonProperty("@graph")
    private List<Object> getGraph() {
        List<Object> list = new ArrayList<>();
        list.add(this.selfMetadata);
        list.addAll(getFiles());
        list.addAll(getAuthors());

        return list;
    }

    @JsonIgnore
    public String getName() {
        return selfMetadata.name;
    }

    public void setName(String name) {
        selfMetadata.name = name;
    }

    @JsonIgnore
    public String getDescription() {
        return selfMetadata.description;
    }

    public void setDescription(String description) {
        selfMetadata.description = description;
    }

    @JsonIgnore
    public String getDatePublished() {
        return selfMetadata.datePublished;
    }

    public void setDatePublished(String datePublished) {
        selfMetadata.datePublished = datePublished;
    }

    @JsonIgnore
    public List<File> getFiles() {
        return this.files;
    }

    public void addFile(String path, String name, URL contentUrl, long contentSize) {
        addFile(new File(path, name, contentUrl, contentSize));
    }

    public void addFile(File file) {
        files.add(file);
        selfMetadata.addFileRef(new Reference(file.getId()));
    }

    @JsonIgnore
    public List<Author> getAuthors() {
        return this.authors;
    }

    public void addAuthor(String name) {
        addAuthor(new Author(name));
    }

    public void addAuthor(Author author) {
        authors.add(author);
        selfMetadata.addAuthorRef(new Reference(author.getId()));
    }
}
