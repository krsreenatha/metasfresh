/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution *
 * Copyright (C) 2009 www.metas.de *
 * This program is free software; you can redistribute it and/or modify it *
 * under the terms version 2 of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. *
 * See the GNU General Public License for more details. *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 *****************************************************************************/
package org.adempiere.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.compiere.model.MQuery;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Evaluatees;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import de.metas.i18n.ITranslatableString;
import de.metas.i18n.ImmutableTranslatableString;
import de.metas.logging.LogManager;

/**
 *
 * @author Tobias Schoeneberg, www.metas.de - FR [ 2897194 ] Advanced Zoom and RelationTypes
 */
public class ZoomInfoFactory
{
	public static final ZoomInfoFactory get()
	{
		return instance;
	}

	private static final transient ZoomInfoFactory instance = new ZoomInfoFactory();

	public static interface IZoomSource
	{
		Properties getCtx();

		Evaluatee createEvaluationContext();

		String getTrxName();

		int getAD_Window_ID();

		String getTableName();

		int getAD_Table_ID();

		String getKeyColumnName();

		List<String> getKeyColumnNames();

		int getRecord_ID();

		default boolean isSOTrx()
		{
			return Env.isSOTrx(getCtx());
		}

		boolean hasField(String columnName);

		Object getFieldValue(String columnName);

		default boolean getFieldValueAsBoolean(final String columnName)
		{
			return DisplayType.toBoolean(getFieldValue(columnName));
		}
	}

	public static final class POZoomSource implements IZoomSource
	{
		public static final POZoomSource of(final PO po, final int adWindowId)
		{
			return new POZoomSource(po, adWindowId);
		}

		public static final POZoomSource of(final PO po)
		{
			final int adWindowId = 0;
			return new POZoomSource(po, adWindowId);
		}

		private final PO po;
		private final int adWindowId;
		private final List<String> keyColumnNames;

		private POZoomSource(final PO po, final int adWindowId)
		{
			Check.assumeNotNull(po, "Parameter po is not null");
			this.po = po;
			this.adWindowId = adWindowId;

			final String[] keyColumnNamesArr = po.get_KeyColumns();
			if (keyColumnNamesArr == null)
			{
				keyColumnNames = ImmutableList.of();
			}
			else
			{
				keyColumnNames = ImmutableList.copyOf(keyColumnNamesArr);
			}
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this)
					.add("po", po)
					.add("AD_Window_ID", adWindowId)
					.toString();
		}

		public PO getPO()
		{
			return po;
		}

		@Override
		public int getAD_Window_ID()
		{
			return adWindowId;
		}

		@Override
		public String getTableName()
		{
			return po.get_TableName();
		}

		@Override
		public int getAD_Table_ID()
		{
			return po.get_Table_ID();
		}

		@Override
		public String getKeyColumnName()
		{
			if (keyColumnNames.size() != 1)
			{
				return null;
			}
			return keyColumnNames.get(0);
		}

		@Override
		public List<String> getKeyColumnNames()
		{
			return keyColumnNames;
		}

		@Override
		public int getRecord_ID()
		{
			return po.get_ID();
		}

		@Override
		public Properties getCtx()
		{
			return po.getCtx();
		}

		@Override
		public String getTrxName()
		{
			return po.get_TrxName();
		}

		@Override
		public Evaluatee createEvaluationContext()
		{
			final Properties privateCtx = Env.deriveCtx(getCtx());

			final PO po = getPO();
			final POInfo poInfo = po.getPOInfo();
			for (int i = 0; i < poInfo.getColumnCount(); i++)
			{
				final Object val;
				final int dispType = poInfo.getColumnDisplayType(i);
				if (DisplayType.isID(dispType))
				{
					// make sure we get a 0 instead of a null for foreign keys
					val = po.get_ValueAsInt(i);
				}
				else
				{
					val = po.get_Value(i);
				}

				if (val == null)
				{
					continue;
				}

				if (val instanceof Integer)
				{
					Env.setContext(privateCtx, "#" + po.get_ColumnName(i), (Integer)val);
				}
				else if (val instanceof String)
				{
					Env.setContext(privateCtx, "#" + po.get_ColumnName(i), (String)val);
				}
			}

			return Evaluatees.ofCtx(privateCtx, Env.WINDOW_None, false);
		}

		@Override
		public boolean hasField(String columnName)
		{
			return po.getPOInfo().hasColumnName(columnName);
		}

		@Override
		public Object getFieldValue(String columnName)
		{
			return po.get_Value(columnName);
		}
	}

	/**
	 * Simple class that contains zoom information. Currently used by
	 * {@link org.compiere.apps.AZoomAcross}.
	 *
	 * @author ts
	 *
	 */
	@SuppressWarnings("serial")
	public static final class ZoomInfo implements Serializable
	{
		public static final ZoomInfo of(final String zoomInfoId, final int windowId, final MQuery query, final ITranslatableString destinationDisplay)
		{
			return new ZoomInfo(zoomInfoId, windowId, query, destinationDisplay);
		}

		private final String _zoomInfoId;
		private final ITranslatableString _destinationDisplay;
		private final MQuery _query;
		private final int _windowId;

		private ZoomInfo(final String zoomInfoId, final int windowId, final MQuery query, final ITranslatableString destinationDisplay)
		{
			super();

			Check.assumeNotEmpty(zoomInfoId, "zoomInfoId is not empty");
			_zoomInfoId = zoomInfoId;

			Check.assume(windowId > 0, "windowId > 0");
			_windowId = windowId;

			Check.assumeNotNull(query, "Parameter query is not null");
			_query = query;

			Check.assumeNotNull(destinationDisplay, "destinationDisplay is not null");
			_destinationDisplay = destinationDisplay;
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this)
					.add("zoomInfoId", _zoomInfoId)
					.add("display", _destinationDisplay)
					.add("AD_Window_ID", _windowId)
					.add("RecordCount", _query.getRecordCount())
					.toString();
		}

		public String getId()
		{
			return _zoomInfoId;
		}

		public ITranslatableString getLabel()
		{
			final ITranslatableString postfix = ImmutableTranslatableString.constant(" (#" + getRecordCount() + ")");
			return ITranslatableString.compose(_destinationDisplay, postfix);
		}

		public int getRecordCount()
		{
			return _query.getRecordCount();
		}

		public int getAD_Window_ID()
		{
			return _windowId;
		}

		public MQuery getQuery()
		{
			return _query;
		}
	}

	private static final Logger logger = LogManager.getLogger(ZoomInfoFactory.class);

	private boolean factAcctZoomProviderEnabled = true;

	private ZoomInfoFactory()
	{
		super();
	}

	/**
	 * @param source the source we need zoom targets for
	 * @return a list of zoom targets. The {@link ZoomInfo#getRecordCount()} of the ZoomInfo's query member might be zero.
	 */
	public List<ZoomInfo> retrieveZoomInfos(final IZoomSource source)
	{
		final int targetAD_Window_ID = -1;
		final boolean checkRecordsCount = true;
		return retrieveZoomInfos(source, targetAD_Window_ID, checkRecordsCount);
	}

	/**
	 * Retrieves all {@link ZoomInfo}s for given {@link IZoomSource}.
	 */
	private List<ZoomInfo> retrieveZoomInfos(final IZoomSource source, final int targetAD_Window_ID, final boolean checkRecordsCount)
	{
		logger.debug("source={}", source);

		final ImmutableList.Builder<ZoomInfo> result = ImmutableList.builder();
		final Set<Integer> alreadySeenWindowIds = new HashSet<>();

		final List<IZoomProvider> zoomProviders = retrieveZoomProviders(source.getTableName());
		for (final IZoomProvider zoomProvider : zoomProviders)
		{
			logger.debug("Checking zoom provider: {}", zoomProvider);

			try
			{
				for (final ZoomInfo zoomInfo : zoomProvider.retrieveZoomInfos(source, targetAD_Window_ID, checkRecordsCount))
				{
					final int adWindowId = zoomInfo.getAD_Window_ID();

					// If not our target window ID, skip it
					// This shall not happen because we asked the zoomProvider to return only those for our target window,
					// but if is happening (because of a bug zoom provider) we shall not be so fragile.
					if (targetAD_Window_ID > 0 && targetAD_Window_ID != adWindowId)
					{
						new AdempiereException("Got a ZoomInfo which is not for our target window. Skipping it."
								+ "\n zoomInfo: " + zoomInfo
								+ "\n zoomProvider: " + zoomProvider
								+ "\n targetAD_Window_ID: " + targetAD_Window_ID
								+ "\n source: " + source
								+ "\n checkRecordsCount: " + checkRecordsCount)
										.throwIfDeveloperModeOrLogWarningElse(logger);
						continue;
					}

					// #1062
					// Only consider a window already seen if it actually has record count > 0
					if (checkRecordsCount && zoomInfo.getRecordCount() > 0)
					{
						if (!alreadySeenWindowIds.add(adWindowId))
						{
							logger.debug("Skipping zoomInfo {} from {} because there is already one for destination '{}'", zoomInfo, zoomProvider, adWindowId);
							continue;
						}
					}
					//
					// Filter out those ZoomInfos which have ZERO records (if requested)
					if (checkRecordsCount && zoomInfo.getRecordCount() <= 0)
					{
						logger.debug("No target records for destination {}", zoomInfo);
						continue;
					}

					logger.debug("Adding zoomInfo {} from {}", zoomInfo, zoomProvider);
					result.add(zoomInfo);
				}
			}
			catch (final Exception e)
			{
				logger.warn("Failed retrieving zoom infos from {} for {}. Skipped.", zoomProvider, source, e);
			}
		}

		return result.build();
	}

	/**
	 * Retrieves that {@link ZoomInfo} which is referencing our given <code>source</code> and has given target window.
	 *
	 * NOTE: Records count is not checked
	 *
	 * @param source
	 * @param targetWindowId target AD_Window_ID (must be provided)
	 * @return ZoomInfo; never returns <code>null</code>
	 */
	public ZoomInfo retrieveZoomInfo(final IZoomSource source, final int targetWindowId)
	{
		Check.assume(targetWindowId > 0, "targetWindowId > 0");

		final boolean checkRecordsCount = false;
		final List<ZoomInfo> zoomInfos = retrieveZoomInfos(source, targetWindowId, checkRecordsCount);
		if (zoomInfos.isEmpty())
		{
			throw new AdempiereException("No zoomInfo found for source=" + source + ", targetWindowId=" + targetWindowId);
		}
		else if (zoomInfos.size() == 1)
		{
			return zoomInfos.get(0);
		}
		else
		{
			// Got more then one Zoominfo(s).
			// we could check if they all have the same AD_Window_ID but does not matter..
			// => returning the first one
			return zoomInfos.get(0);
		}
	}

	private List<IZoomProvider> retrieveZoomProviders(final String tableName)
	{
		final List<IZoomProvider> zoomProviders = new ArrayList<>();

		// NOTE: Zoom providers order matter because in case it finds some duplicates (i.e. same window),
		// it will pick only the first one (i.e. the one from the first provider).
		zoomProviders.addAll(RelationTypeZoomProvidersFactory.instance.getZoomProvidersBySourceTableName(tableName));
		zoomProviders.add(GenericZoomProvider.instance);
		if (factAcctZoomProviderEnabled)
		{
			zoomProviders.add(FactAcctZoomProvider.instance);
		}

		return zoomProviders;
	}

	/**
	 * Disable the {@link FactAcctZoomProvider} (which is enabled by default
	 * 
	 * @deprecated Needed only for Swing
	 */
	@Deprecated
	public void disableFactAcctZoomProvider()
	{
		factAcctZoomProviderEnabled = false;
	}
}
