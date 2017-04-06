package de.metas.manufacturing.dispo.event;

import org.adempiere.model.PlainContextAware;

import de.metas.adempiere.model.I_C_Order;
import de.metas.event.Event;
import de.metas.event.IEventBus;
import de.metas.event.IEventListener;

/*
 * #%L
 * metasfresh-manufacturing-dispo
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class OrderProcessedEventListener implements IEventListener
{

	@Override
	public void onEvent(final IEventBus eventBus, final Event event)
	{
		final PlainContextAware ctx = PlainContextAware.newWithThreadInheritedTrx();
		final I_C_Order order = event.getRecord().getModel(ctx, I_C_Order.class);

	}

}