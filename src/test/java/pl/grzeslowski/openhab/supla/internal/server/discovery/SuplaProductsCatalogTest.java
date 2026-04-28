package pl.grzeslowski.openhab.supla.internal.server.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SuplaProductsCatalogTest {
    @Test
    void shouldResolveProductByManufacturerAndProductId() {
        var product = SuplaProductsCatalog.findProductInfo(4, 9000);
        var differentManufacturerSameProductId = SuplaProductsCatalog.findProductInfo(19, 1);

        assertThat(product).isPresent();
        assertThat(product.get().manufacturer()).isEqualTo("ZAMEL");
        assertThat(product.get().name()).isEqualTo("ZAMEL mSLW-01");
        assertThat(product.get().description()).isEqualTo("ZAMEL mSLW-01");
        assertThat(differentManufacturerSameProductId).isPresent();
        assertThat(differentManufacturerSameProductId.get().name())
                .isNotEqualTo(product.get().name());
    }

    @Test
    void shouldNotResolveWithMissingOrLegacyIds() {
        assertThat(SuplaProductsCatalog.findProductInfo(null, 9000)).isEmpty();
        assertThat(SuplaProductsCatalog.findProductInfo(4, null)).isEmpty();
        assertThat(SuplaProductsCatalog.findProductInfo(0, 0)).isEmpty();
    }
}
