package org.crittr.shared.browser.utilities;

import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;

public abstract class RankDependentRunnable implements Runnable {

	protected KingdomRankResult ikrp = null;
	
	public void setKingdomRank(KingdomRankResult ikrp) {
		this.ikrp = ikrp;
	}
}