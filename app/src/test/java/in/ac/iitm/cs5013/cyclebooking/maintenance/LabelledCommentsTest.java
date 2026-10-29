package in.ac.iitm.cs5013.cyclebooking.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The classifier's training set (week 5) is well formed: every row has a known label,
 * no text appears twice, and each label has enough examples to learn from.
 */
class LabelledCommentsTest {

    /** Every part's label, plus "none". A label that isn't a part fails here (ADR 0005). */
    private static final Set<String> LABELS = Stream.concat(
            Arrays.stream(Part.values()).map(Part::label), Stream.of("none")).collect(Collectors.toSet());

    private record Row(List<String> labels, String text, String source) {
    }

    private static List<Row> load() throws IOException {
        List<Row> rows = new ArrayList<>();
        try (InputStream in = LabelledCommentsTest.class.getResourceAsStream("/maintenance/labelled-comments.tsv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    assertThat(line).isEqualTo("labels\ttext\tsource");
                    header = false;
                    continue;
                }
                String[] cols = line.split("\t", -1);
                assertThat(cols).as(line).hasSize(3);
                rows.add(new Row(List.of(cols[0].split(",")), cols[1], cols[2]));
            }
        }
        return rows;
    }

    @Test
    void datasetHasAtLeast100RowsWithKnownLabels() throws IOException {
        List<Row> rows = load();

        assertThat(rows).hasSizeGreaterThanOrEqualTo(100);
        for (Row row : rows) {
            assertThat(LABELS).as(row.text()).containsAll(row.labels());
            assertThat(row.text()).isNotBlank();
            assertThat(row.source()).isIn("seed", "pilot");
            if (row.labels().contains("none")) {
                assertThat(row.labels()).as("'none' cannot be combined: " + row.text()).hasSize(1);
            }
        }
    }

    @Test
    void everyLabelHasAtLeastTenExamplesAndNoTextRepeats() throws IOException {
        Map<String, Integer> perLabel = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (Row row : load()) {
            assertThat(seen.add(row.text().toLowerCase())).as("duplicate: " + row.text()).isTrue();
            row.labels().forEach(label -> perLabel.merge(label, 1, Integer::sum));
        }

        for (String label : LABELS) {
            assertThat(perLabel.getOrDefault(label, 0)).as(label).isGreaterThanOrEqualTo(10);
        }
    }
}
