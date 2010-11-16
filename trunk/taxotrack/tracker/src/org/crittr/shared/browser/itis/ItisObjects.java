package org.crittr.shared.browser.itis;

import java.util.Map;

import org.crittr.shared.browser.Constants;

public class ItisObjects {

	
	public static class KingdomRankResult {

        public String rank_name, kingdom_name;
		public int kingdom_id, rank_id;

		public KingdomRankResult() {}
		
		public KingdomRankResult(Map<String, String> hashmap) {
			kingdom_id = Integer.parseInt( hashmap.get(ItisUtils.KINGDOM_ID_TAG) );
	        rank_id = Integer.parseInt( hashmap.get(ItisUtils.RANK_ID_TAG) );
	        kingdom_name = hashmap.get(ItisUtils.KINGDOM_NAME_TAG);
	        rank_name = hashmap.get(ItisUtils.RANK_NAME_TAG);
		}
	}
	
	
	public static class ScientificNameSearchResult {
		public String combined_name, unit1, unit2, unit3, unit4, genus, species, subspecies, subsubspecies;
		public long tsn;

		public ScientificNameSearchResult() {}
		
		public ScientificNameSearchResult(Map<String, String> hashmap) {
			combined_name = hashmap.get(ItisUtils.COMBINED_NAME_TAG);
			unit1 = hashmap.get(ItisUtils.UNIT_1_TAG);
			unit2 = hashmap.get(ItisUtils.UNIT_2_TAG);
			unit3 = hashmap.get(ItisUtils.UNIT_3_TAG);
			unit4 = hashmap.get(ItisUtils.UNIT_4_TAG);
			genus = hashmap.get(ItisUtils.GENUS_TAG);
			species = hashmap.get(ItisUtils.SPECIES_TAG);
			subspecies = hashmap.get(ItisUtils.SUBSPECIES_TAG);
			subsubspecies = hashmap.get(ItisUtils.SUBSUBSPECIES_TAG);
			tsn = Long.parseLong( hashmap.get(ItisUtils.TSN_TAG) );
		}
	}
	
	public static class CommonNameSearchResult {
		public String common_name, language;
		public long tsn;

		public CommonNameSearchResult() {}
		
		public CommonNameSearchResult(Map<String, String> hashmap) {
			common_name = hashmap.get(ItisUtils.COMMON_NAME_TAG);
			language = hashmap.get(ItisUtils.LANGUAGE_TAG);
			tsn = Long.parseLong( hashmap.get(ItisUtils.TSN_TAG) );
		}
	}
	
	
	public static class HierarchyResult {
		public String parent_taxon_name, rank_name, taxon_name;
		public long tsn, parent_tsn;
		
		public HierarchyResult() {}
		
		public HierarchyResult(Map<String, String> hashmap) {
			parent_taxon_name = hashmap.get(ItisUtils.PARENT_NAME_TAG);
			rank_name = hashmap.get(ItisUtils.RANK_NAME_TAG);
			taxon_name = hashmap.get(ItisUtils.TAXON_NAME_TAG);
			tsn = Long.parseLong( hashmap.get(ItisUtils.TSN_TAG) );
			
			String number = hashmap.get(ItisUtils.PARENT_TSN_TAG);
			if (number == null || number.length() == 0)
				parent_tsn = Constants.NO_PARENT_ID;
			else
				parent_tsn = Long.parseLong( number );
		}
	}
}
