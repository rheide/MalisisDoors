package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.nio.file.*;
import net.malisis.doors.*;
import net.malisis.doors.factory.*;
import net.malisis.doors.field.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;

public final class PersistenceVerification {
  public static void run(ServerLevel level) throws Exception {
    for (int i = 0; i < 88; i++) {
      var pos = ServerVerification.root(i);
      check(level.getBlockEntity(pos) instanceof DoorBlockEntity, "saved standard entity " + i);
      check(
          !level.getBlockState(pos.above()).hasBlockEntity(),
          "saved upper owns no duplicate entity");
    }
    int i = 0;
    for (var block : Mechanisms.BLOCKS)
      for (Direction facing : Direction.Plane.HORIZONTAL) {
        var root = new BlockPos((i % 8) * 8, 64, 48 + (i / 8) * 8);
        i++;
        check(
            level.getBlockEntity(root) instanceof MechanismEntity, "saved mechanism root " + root);
        for (var cell : block.get().footprint(root, level.getBlockState(root)))
          if (!cell.equals(root))
            check(
                level.getBlockEntity(cell) instanceof PartEntity part && part.root.equals(root),
                "saved multiblock proxy");
      }
    var factory = (FactoryEntity) level.getBlockEntity(FactoryVerification.FACTORY);
    check(
        factory != null && factory.getItem(0).getCount() == 2 && factory.getItem(4).getCount() == 1,
        "saved real Factory transaction inventory");
    var coded = (DoorBlockEntity) level.getBlockEntity(FactoryVerification.CODED);
    check(coded.configuration.code().equals("123456"), "saved door access code");
    for (int field = 0; field < 3; field++) {
      var root = new BlockPos(336 + 8 * field, 64, 0);
      check(level.getBlockEntity(root) instanceof FieldEntity, "saved forcefield controller");
      var controller = (FieldEntity) level.getBlockEntity(root);
      check(controller.area() == 9, "saved field bounds");
      for (var pos : BlockPos.betweenClosed(controller.root, controller.max))
        check(
            level.getBlockEntity(pos) instanceof FieldEntity part
                && part.identity.equals(controller.identity),
            "saved field member identity");
    }
    Files.writeString(
        Path.of(System.getProperty("malisis.verify.out"), "persistence-verification.txt"),
        "PASS "
            + ServerVerification.checks
            + " saved-world checks after clean server restart; standard doors, upper ownership, all"
            + " mechanism roots/proxies, Factory transaction inventory, access code and every"
            + " forcefield identity\n");
  }
}
