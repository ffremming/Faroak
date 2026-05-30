package resources.testing.probes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GameContext;
import resources.presentation.image.ImageContainer;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the load-time art aliasing for farming:
 *   - legacy {@code crop_wheat}/{@code crop_carrot} stages (no art on disk) borrow
 *     an existing fantasy crop's stage sprite, so a planted legacy seed is visible.
 *   - harvest produce items ({@code wheat}, {@code emberwheat}, …) borrow their
 *     mature crop sprite, so dropped produce is visible instead of a "?" swatch.
 *
 * Detects the regression by comparing against the shared missing-art placeholder:
 * a name with no art and no alias resolves to the same placeholder instance an
 * unknown name does. The aliased names must resolve to something else.
 */
public final class CropArtAliasProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CropArtAliasProbe.class);

    @Override public String name() { return "crop-art-alias"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        ImageContainer images = ctx.player().panel.imageContainer;

        // Reference placeholder: an object/item name guaranteed to have no art.
        BufferedImage objPlaceholder = first(images.getObjectImages("definitely_missing_object_xyz"));
        BufferedImage itemPlaceholder = images.getItemImage("definitely_missing_item_xyz");

        BufferedImage wheatStage0  = first(images.getObjectImages("crop_wheat_stage0"));
        BufferedImage carrotStage0 = first(images.getObjectImages("crop_carrot_stage0"));
        BufferedImage producEmber  = images.getItemImage("emberwheat");
        BufferedImage produceWheat  = images.getItemImage("wheat");

        boolean wheatOk  = wheatStage0  != null && wheatStage0  != objPlaceholder;
        boolean carrotOk = carrotStage0 != null && carrotStage0 != objPlaceholder;
        boolean emberOk  = producEmber  != null && producEmber  != itemPlaceholder;
        boolean wheatItemOk = produceWheat != null && produceWheat != itemPlaceholder;

        String detail = String.format(
            "crop_wheat_stage0=%s, crop_carrot_stage0=%s, item:emberwheat=%s, item:wheat=%s",
            wheatOk, carrotOk, emberOk, wheatItemOk);
        LOG.info(detail);

        if (!(wheatOk && carrotOk && emberOk && wheatItemOk)) {
            return ProbeResult.fail(name() + " missing farming art alias", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static BufferedImage first(ArrayList<BufferedImage> stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(0);
    }
}
