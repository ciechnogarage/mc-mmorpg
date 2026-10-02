package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PanelMathTest {
  private static List<Integer> numbers(int count) {
    return IntStream.range(0, count).boxed().toList();
  }

  @Test void stronyDlaPustejListy() {
    PanelMath.Page<Integer> page = PanelMath.page(numbers(0), 0);
    assertEquals(1, page.total());
    assertEquals(0, page.items().size());
    assertFalse(page.previous());
    assertFalse(page.next());
  }

  @Test void dwadziesciaSiedemMiesciSieBezStronicowania() {
    PanelMath.Page<Integer> page = PanelMath.page(numbers(27), 0);
    assertEquals(numbers(27), page.items());
    assertFalse(page.next());
  }

  @Test void dwadziesciaOsiemTworzyDwieStrony() {
    PanelMath.Page<Integer> first = PanelMath.page(numbers(28), 0);
    PanelMath.Page<Integer> second = PanelMath.page(numbers(28), 1);
    assertEquals(numbers(28).subList(0, 26), first.items());
    assertTrue(first.next());
    assertFalse(first.previous());
    assertEquals(numbers(28).subList(26, 28), second.items());
    assertTrue(second.previous());
    assertFalse(second.next());
    assertNull(first.marker(26));
    assertNull(second.marker(0));
    assertEquals(26, second.marker(1));
  }

  @Test void szescdziesiatTworzyTrzyStrony() {
    List<Integer> values = numbers(60);
    assertEquals(values.subList(0, 26), PanelMath.page(values, 0).items());
    PanelMath.Page<Integer> middle = PanelMath.page(values, 1);
    assertEquals(values.subList(26, 51), middle.items());
    assertTrue(middle.previous());
    assertTrue(middle.next());
    PanelMath.Page<Integer> last = PanelMath.page(values, 2);
    assertEquals(values.subList(51, 60), last.items());
    assertEquals(3, last.total());
    assertFalse(last.next());
  }

  @Test void filtrPrzechodziPrzezCzteryOpcje() {
    PanelMath.Filter filter = PanelMath.Filter.ALL;
    for (int i = 0; i < 4; i++) filter = filter.next();
    assertEquals(PanelMath.Filter.ALL, filter);
    assertTrue(PanelMath.Filter.PETS.accepts("strefa"));
    assertTrue(PanelMath.Filter.PETS.accepts("legendarny"));
    assertFalse(PanelMath.Filter.PETS.accepts("miasto"));
    assertTrue(PanelMath.Filter.CITY.accepts("start"));
    assertTrue(PanelMath.Filter.OWN.accepts("wlasny"));
  }

  @Test void suroweSlotyPaskaMapujaAkcje() {
    assertEquals(PanelMath.Action.ADD, PanelMath.action(81));
    assertEquals(PanelMath.Action.FILTER, PanelMath.action(82));
    assertEquals(PanelMath.Action.NAVIGATION, PanelMath.action(83));
    assertEquals(PanelMath.Action.MINIMAP, PanelMath.action(84));
    assertEquals(PanelMath.Action.FULL, PanelMath.action(88));
    assertEquals(PanelMath.Action.CLOSE, PanelMath.action(89));
    assertEquals(PanelMath.Action.NONE, PanelMath.action(85));
    assertEquals(PanelMath.Action.NONE, PanelMath.action(54));
  }
}
