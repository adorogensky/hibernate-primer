/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.adorogensky.workshop.hibernate.data.jpa.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.github.adorogensky.workshop.hibernate.data.jpa.domain.City;
import com.github.adorogensky.workshop.hibernate.data.jpa.domain.Hotel;
import com.github.adorogensky.workshop.hibernate.data.jpa.domain.Rating;
import com.github.adorogensky.workshop.hibernate.data.jpa.domain.RatingCount;
import com.github.adorogensky.workshop.hibernate.data.jpa.domain.Review;
import com.github.adorogensky.workshop.hibernate.data.jpa.domain.ReviewDetails;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component("hotelService")
@Transactional
class HotelServiceImpl implements HotelService {

	private final HotelRepository hotelRepository;

	private final ReviewRepository reviewRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	public HotelServiceImpl(HotelRepository hotelRepository,
			ReviewRepository reviewRepository) {
		this.hotelRepository = hotelRepository;
		this.reviewRepository = reviewRepository;
	}

	@Override
	public Hotel getHotel(City city, String name) {
		Assert.notNull(city, "City must not be null");
		Assert.hasLength(name, "Name must not be empty");
		return this.hotelRepository.findByCityAndName(city, name);
	}

	@Override
	public Page<Review> getReviews(Hotel hotel, Pageable pageable) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotel(hotel, pageable);
	}

	@Override
	public Review getReview(Hotel hotel, int reviewNumber) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotelAndIndex(hotel, reviewNumber);
	}

	@Override
	public Review addReview(Hotel hotel, ReviewDetails details) {
		Review review = new Review(hotel, 1, details);
		return reviewRepository.save(review);
	}

	@Override
	public Review addReview(Long hotelId, Review review) {
		Hotel hotel = hotelRepository.findById(hotelId);
		hotel.getReviews().add(review);
		review.setHotel(hotel);

		// This line will not make persistent and review.id WILL NOT be populated
		//hotelRepository.save(hotel);

		// This line will make the new review object persistent and review.id will be populated
		//entityManager.persist(hotel);

		// The new review object will be made persistent and review.id will be populated

		System.out.println("entityManager.getFlushMode() = " + entityManager.getFlushMode());

		return review;
	}

	@Override
	public ReviewsSummary getReviewSummary(Hotel hotel) {
		List<RatingCount> ratingCounts = this.hotelRepository.findRatingCounts(hotel);
		return new ReviewsSummaryImpl(ratingCounts);
	}

	private static class ReviewsSummaryImpl implements ReviewsSummary {

		private final Map<Rating, Long> ratingCount;

		public ReviewsSummaryImpl(List<RatingCount> ratingCounts) {
			this.ratingCount = new HashMap<Rating, Long>();
			for (RatingCount ratingCount : ratingCounts) {
				this.ratingCount.put(ratingCount.getRating(), ratingCount.getCount());
			}
		}

		@Override
		public long getNumberOfReviewsWithRating(Rating rating) {
			Long count = this.ratingCount.get(rating);
			return count == null ? 0 : count;
		}
	}
}
