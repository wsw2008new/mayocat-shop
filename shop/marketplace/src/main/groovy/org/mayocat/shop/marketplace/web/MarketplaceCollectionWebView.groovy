/**
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.shop.marketplace.web

import com.google.common.base.Optional
import groovy.transform.CompileStatic
import org.mayocat.entity.EntityData
import org.mayocat.entity.EntityDataLoader
import org.mayocat.entity.StandardOptions
import org.mayocat.image.model.Image
import org.mayocat.image.model.ImageGallery
import org.mayocat.model.EntityAndParent
import org.mayocat.rest.Resource
import org.mayocat.shop.catalog.model.Collection
import org.mayocat.shop.catalog.store.CollectionStore
import org.mayocat.shop.catalog.web.object.CollectionWebObject
import org.mayocat.shop.front.views.WebView
import org.mayocat.shop.marketplace.web.object.BreadcrumbElementWebObject
import org.mayocat.url.EntityURLFactory
import org.xwiki.component.annotation.Component

import javax.inject.Inject
import javax.inject.Provider
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @version $Id$
 */
@Component("/marketplace/collections")
@Path("/marketplace/collections")
@Produces([MediaType.TEXT_HTML, MediaType.APPLICATION_JSON])
@CompileStatic
class MarketplaceCollectionWebView implements Resource
{
    @Inject
    EntityDataLoader dataLoader

    @Inject
    Provider<CollectionStore> collectionStore

    @Inject
    EntityURLFactory urlFactory

    @GET
    @Path("{parent3}/{parent2}/{parent1}/{slug}")
    def getCollectionWithThreeParents(
            @PathParam("parent3") String parent3,
            @PathParam("parent2") String parent2,
            @PathParam("parent1") String parent1,
            @PathParam("slug") String slug,
            @QueryParam("page") @DefaultValue("1") Integer page)
    {
        return getCollectionInternal(parent3, parent2, parent1, slug)
    }

    @GET
    @Path("{parent2}/{parent1}/{slug}")
    def getCollectionWithTwoParents(
            @PathParam("parent2") String parent2,
            @PathParam("parent1") String parent1,
            @PathParam("slug") String slug,
            @QueryParam("page") @DefaultValue("1") Integer page)
    {
        return getCollectionInternal(parent2, parent1, slug)
    }

    @GET
    @Path("{parent1}/{slug}")
    def getCollectionWithOneParent(
            @PathParam("parent1") String parent1,
            @PathParam("slug") String slug,
            @QueryParam("page") @DefaultValue("1") Integer page)
    {
        return getCollectionInternal(parent1, slug)
    }

    @GET
    @Path("{slug}")
    def getCollection(@PathParam("slug") String slug, @QueryParam("page") @DefaultValue("1") Integer page)
    {
        return getCollectionInternal(slug)
    }

    def getCollectionInternal(String... slugs)
    {
        def context = [:]

        final EntityAndParent<Collection> collectionChain = collectionStore.get().findBySlugs(slugs)

        if (collectionChain == null) {
            return Response.status(Response.Status.NOT_FOUND).entity([
                    error: new org.mayocat.rest.error.Error(Response.Status.NOT_FOUND, "Collection does not exist")
            ]).build()
        }

        List<BreadcrumbElementWebObject> breadcrumbElements = []
        EntityAndParent<Collection> element = collectionChain
        while (true) {
            breadcrumbElements << new BreadcrumbElementWebObject([
                    title: element.entity.title,
                    url  : serializeUrl(element)
            ])
            if (element.parent == null) {
                break
            }
            element = element.parent
        }

        context.put("breadcrumb", breadcrumbElements.reverse())

        final Collection collection = collectionChain.entity

        EntityData<org.mayocat.shop.catalog.model.Collection> data = dataLoader.
                load(collection, StandardOptions.LOCALIZE)

        Optional<ImageGallery> gallery = data.getData(ImageGallery.class)
        List<Image> images = gallery.isPresent() ? gallery.get().images : [] as List<Image>

        CollectionWebObject collectionWebObject = new CollectionWebObject()
        collectionWebObject.withCollection(data.entity, urlFactory)

        collectionWebObject.withImages(images, collection.featuredImageId, Optional.absent())

        context.put("collection", collectionWebObject)

        new WebView().data(context)
    }

    String serializeUrl(EntityAndParent<Collection> collection)
    {
        String path = ""
        EntityAndParent<Collection> item = collection
        while (true) {
            path = "${item.entity.slug}${path == '' ? '' : '/'}${path}"
            if (item.parent == null) {
                break
            }
            item = item.parent
        }
        return "/collections/" + path
    }
}
