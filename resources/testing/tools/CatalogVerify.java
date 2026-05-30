package resources.testing.tools;

import java.io.FileWriter;
import java.io.IOException;

import resources.domain.object.ObjectCatalog;

/**
 * Tiny offline self-check that does NOT depend on the game runtime or the
 * testing Logger: confirms the ObjectCatalog manifest matches the sliced PNGs
 * on disk and that names are unique. Writes a PASS/FAIL report to a file so the
 * result survives a flaky stdout channel.
 *
 * Run: java -cp out resources.testing.tools.CatalogVerify /tmp/catalog_verify.txt
 */
public final class CatalogVerify {

    public static void main(String[] args) throws IOException {
        String outPath = args.length > 0 ? args[0] : "catalog_verify.txt";
        StringBuilder sb = new StringBuilder();
        int problems = 0;

        java.util.Set<String> names = new java.util.HashSet<>();
        int missingArt = 0, dupes = 0;
        java.util.List<ObjectCatalog.Entry> all = new java.util.ArrayList<>();
        all.addAll(ObjectCatalog.ENTRIES);
        all.addAll(ObjectCatalog.EXTRA);

        for (ObjectCatalog.Entry e : all) {
            if (!names.add(e.name)) { dupes++; sb.append("DUP name: ").append(e.name).append('\n'); }
            java.io.File png = new java.io.File(
                "resources/images/objects/" + e.category + "/" + e.name + "/" + e.name + ".png");
            if (!png.isFile()) { missingArt++; sb.append("MISSING art: ").append(png.getPath()).append('\n'); }
        }
        problems += dupes + missingArt;

        sb.append("entries=").append(ObjectCatalog.ENTRIES.size())
          .append(" extra=").append(ObjectCatalog.EXTRA.size())
          .append(" total=").append(all.size())
          .append(" dupes=").append(dupes)
          .append(" missingArt=").append(missingArt)
          .append('\n');
        sb.append(problems == 0 ? "RESULT: PASS\n" : "RESULT: FAIL (" + problems + " problems)\n");

        try (FileWriter w = new FileWriter(outPath)) {
            w.write(sb.toString());
        }
    }
}
